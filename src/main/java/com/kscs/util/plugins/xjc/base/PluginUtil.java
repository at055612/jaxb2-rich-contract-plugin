/*
 * MIT License
 *
 * Copyright (c) 2014 Klemm Software Consulting, Mirko Klemm
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.kscs.util.plugins.xjc.base;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.kscs.util.plugins.xjc.outline.PropertyOutline;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.outline.FieldOutline;

/**
 * Common utilities for XJC plugins
 */
public final class PluginUtil {
	private static final ResourceBundle resourceBundle = PropertyDirectoryResourceBundle.getInstance(PluginUtil.class);

	private static String getMessage(final String key, final Object... args) {
		return MessageFormat.format(PluginUtil.resourceBundle.getString(key), args);
	}

	private PluginUtil() {
	}

	public static JFieldVar getDeclaredField(final FieldOutline fieldOutline) {
		return fieldOutline.parent().implClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
	}

	public static JExpression nullSafe(final JExpression test, final JExpression source) {
		return JOp.cond(test.eq(JExpr._null()), JExpr._null(), source);
	}

	public static JExpression nullSafe(final FieldOutline test, final JExpression source) {
		return nullSafe(JExpr.ref(test.getPropertyInfo().getName(false)), source);
	}

	public static JExpression nullSafe(final PropertyOutline test, final JExpression source) {
		return nullSafe(JExpr.ref(test.getFieldName()), source);
	}

	public static JType getElementType(final FieldOutline fieldOutline) {
		final JFieldVar definedField = PluginUtil.getDeclaredField(fieldOutline);
		if (definedField != null) {
			if (fieldOutline.getPropertyInfo().isCollection()) {
				return definedField.type().isArray() ? definedField.type().elementType() : ((JClass) definedField.type()).getTypeParameters().get(0);
			} else {
				return definedField.type();
			}
		} else {
			return null;
		}
	}

	public static <I,T extends I> T getConfiguredObject(final Class<I> interfaceClass, final Class<T> defaultClass) {
		final String strategyClassName = System.getProperty(interfaceClass.getName());
		Class<T> strategyClass;
		if (strategyClassName == null || strategyClassName.trim().isEmpty()) {
			strategyClass = defaultClass;
		} else {
			try {
				strategyClass = ((Class<T>)Class.forName(strategyClassName));
			} catch (final ClassNotFoundException cnfe) {
				strategyClass = defaultClass;
			}
		}
		try {
			return strategyClass.getConstructor().newInstance();
		} catch (final NoSuchMethodException e) {
			throw new RuntimeException(getMessage("error.no-such-constructor", strategyClassName), e);
		} catch (final InvocationTargetException | InstantiationException | IllegalAccessException ex) {
			throw new RuntimeException(getMessage("error.cannot-instatiate-strategy", strategyClassName), ex);
		}
	}
}
