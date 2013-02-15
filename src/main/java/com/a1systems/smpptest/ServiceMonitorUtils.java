package com.a1systems.smpptest;

import java.util.List;

public class ServiceMonitorUtils {
	public static boolean isAllStopped(List<ServiceMonitor> monitors){
		boolean fl = true;

		for (ServiceMonitor mon:monitors) {
			fl &= mon.isStopped();

			if (!fl) {
				return fl;
			}
		}

		return fl;
	}

	public static boolean isAllWorking(List<ServiceMonitor> monitors){
		boolean fl = true;

		for (ServiceMonitor mon:monitors) {
			fl &= mon.isWorking();

			if (!fl) {
				return fl;
			}
		}

		return fl;
	}

	public static void waitStopped(ServiceMonitor monitor) throws InterruptedException{
		while (!monitor.isStopped()) {
			Thread.sleep(200);
		}
	}

	public static void waitAllStopped(List<ServiceMonitor> monitors) throws InterruptedException{
		while (!ServiceMonitorUtils.isAllStopped(monitors)) {
			Thread.sleep(200);
		}
	}

	public static void stoppingAll(List<ServiceMonitor> monitors) {
		for (ServiceMonitor mon:monitors) {
			mon.stopping();
		}
	}

	static void waitStopping(ServiceMonitor monitor) throws InterruptedException {
		while (
			!monitor.isStopping()
			&& !monitor.isStopped()
		) {
			Thread.sleep(200);
		}
	}

	static void waitWorking(ServiceMonitor monitor) throws InterruptedException{
		while (!monitor.isWorking()) {
			Thread.sleep(200);
		}
	}

	static void waitAllWorking(List<ServiceMonitor> childMonitors) throws InterruptedException {
		while (!isAllWorking(childMonitors)) {
			Thread.sleep(200);
		}
	}
}
