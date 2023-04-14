package net.azureaaron.jvmci;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class JVMNatives {
	private static final MethodHandle METHOD_FIND_NATIVE = JVMNatives.getMethodFindNativeImpl();

	public static long findNativeFunctionAddress(String name) {
		try {
			return (long) METHOD_FIND_NATIVE.invokeExact(JVMInternals.class.getClassLoader(), name);
		} catch (Throwable e) {
			throw new RuntimeException("Failed to invoke ClassLoader.findNative", e);
		}
	}

	private static MethodHandle getMethodFindNativeImpl() {
		try {
			return JVMInternals.trustedLookup().findStatic(ClassLoader.class, "findNative",
					MethodType.methodType(Long.TYPE, ClassLoader.class, String.class));
		} catch (Throwable e) {
			throw new RuntimeException("Could not expose ClassLoader.findNative", e);
		}
	}
}
