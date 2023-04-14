package net.azureaaron.jvmci;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class JVMInternals {
	private static final Unsafe UNSAFE;
	private static final MethodHandles.Lookup LOOKUP;

	private static MethodHandles.Lookup getTrustedLookup() {
		try {
			Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			return (MethodHandles.Lookup) UNSAFE.getObject(UNSAFE.staticFieldBase(field),
					UNSAFE.staticFieldOffset(field));
		} catch (Throwable t) {
			throw new RuntimeException("Could not access MethodHandles.Lookup.IMPL_LOOKUP", t);
		}
	}

	public static MethodHandles.Lookup trustedLookup() {
		return LOOKUP;
	}

	static {
		try {
			Field field = Unsafe.class.getDeclaredField("theUnsafe");
			field.setAccessible(true);
			UNSAFE = (Unsafe) field.get(null);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("Failed to obtain sun.misc.Unsafe instance", e);
		}
		LOOKUP = JVMInternals.getTrustedLookup();
	}
}
