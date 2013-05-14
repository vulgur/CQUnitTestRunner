package com.adobe.cqunittest;


import org.apache.sling.junit.Activator;
import org.apache.sling.junit.annotations.SlingAnnotationsTestRunner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.osgi.framework.BundleContext;

public class CQUnitTestRunner extends SlingAnnotationsTestRunner {
	private Class<?> klass;

	public CQUnitTestRunner(Class<?> clazz) throws InitializationError {
		super(clazz);
		klass = clazz;
	}

	@Override
	public void run(RunNotifier notifier) {
		boolean isLocal = false;
		final BundleContext ctx = Activator.getBundleContext();
		if (ctx == null) {
			isLocal = true;
		}
		if (isLocal) {
			try {
				// make bundle by maven plugin
				boolean isSucc = CQUnitTestUtil.executeMvn();
				if (isSucc) {
					// install the bundle to CQ
					boolean isActive = CQUnitTestUtil.installBundle();
					if (isActive) {
						// call the test request and print the result
						CQUnitTestUtil.runTest(klass);
					} else {
						System.err.println("Test terminated!");
					}
				} else {
					System.err.println("Test terminated!");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			super.run(notifier);
		}
	}

}
