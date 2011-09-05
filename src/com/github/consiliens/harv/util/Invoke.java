/*******************************************************************************
 * Copyright (c) 2011 consiliens (consiliens@gmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package com.github.consiliens.harv.util;

import java.lang.reflect.Method;

/**
 * Helper methods to work around Vega's current lack of a database API.
 */
public abstract class Invoke {

    public static Object invoke(final Object target, final String methodName, final Object methodParameter) {
        try {
            final Method method = target.getClass().getDeclaredMethod(methodName,
                    methodParameter.getClass().getInterfaces()[0]);
            method.setAccessible(true);
            return method.invoke(target, methodParameter);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static Object invokeStatic(final Class<?> klass, final String methodName, final Object methodParameter) {
        try {
            final Method method = klass.getDeclaredMethod(methodName, methodParameter.getClass());
            method.setAccessible(true);
            return method.invoke(null, methodParameter);
        } catch (final Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}