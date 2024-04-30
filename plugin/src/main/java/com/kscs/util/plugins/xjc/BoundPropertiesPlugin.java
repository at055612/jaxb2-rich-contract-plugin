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
package com.kscs.util.plugins.xjc;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.util.ArrayList;
import com.kscs.util.jaxb.BoundList;
import com.kscs.util.jaxb.BoundListProxy;
import com.kscs.util.jaxb.CollectionChangeEvent;
import com.kscs.util.jaxb.CollectionChangeEventType;
import com.kscs.util.jaxb.CollectionChangeListener;
import com.kscs.util.jaxb.VetoableCollectionChangeListener;
import com.kscs.util.plugins.xjc.base.AbstractPlugin;
import com.kscs.util.plugins.xjc.base.Opt;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.FieldOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * XJC Plugin generates constrained and bound JavaBeans properties
 */
public class BoundPropertiesPlugin extends AbstractPlugin {

	public static final String PROXY_SUFFIX = "__Proxy";
	public static final String SUPPORT_FIELD_SUFFIX = "__Support";
	public static final String OLD_VALUE_VAR_NAME = "__oldValue";
	@Opt private boolean constrained = true;
	@Opt private boolean bound = true;
	@Opt private boolean setterThrows = false;
	@Opt private boolean generateTools = true;

	@Override
	public String getOptionName() {
		return "Xconstrained-properties";
	}

	@Override
	public boolean run(final Outline outline, final Options opt, final ErrorHandler errorHandler) throws SAXException {

		if (!this.constrained && !this.bound) {
			return true;
		}

		final PluginContext pluginContext = PluginContext.get(outline, opt, errorHandler);

		final JCodeModel m = outline.getCodeModel();

		if (this.generateTools) {
			// generate bound collection helper classes
			pluginContext.writeSourceFile(BoundList.class);
			pluginContext.writeSourceFile(BoundListProxy.class);
			pluginContext.writeSourceFile(CollectionChangeEventType.class);
			pluginContext.writeSourceFile(CollectionChangeEvent.class);
			pluginContext.writeSourceFile(CollectionChangeListener.class);
			pluginContext.writeSourceFile(VetoableCollectionChangeListener.class);
		}

		if(pluginContext.hasPlugin(ImmutablePlugin.class)) {
			errorHandler.error(new SAXParseException(getMessage("error.immutableAndConstrainedProperties"), outline.getModel().getLocator()));
		}

		final int setterAccess = JMod.PUBLIC;

		for (final ClassOutline classOutline : outline.getClasses()) {
			final JDefinedClass definedClass = classOutline.implClass;

			// Create bound collection proxies
			for (final FieldOutline fieldOutline : classOutline.getDeclaredFields()) {
				if (fieldOutline.getPropertyInfo().isCollection() && !definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false)).type().isArray()) {
					generateProxyField(classOutline, fieldOutline);
					generateLazyProxyInitGetter(classOutline, fieldOutline);
				}
			}


			if (this.constrained && this.setterThrows) {
				for (final JMethod method : definedClass.methods()) {
					if (method.name().startsWith("with")
							&& !"withVetoableChangeListener".equals(method.name())
							&& !"withPropertyChangeListener".equals(method.name())
							) {
						method._throws(PropertyVetoException.class);
					}
				}
			}

			if (this.constrained)
				createSupportProperty(outline, classOutline, VetoableChangeSupport.class, VetoableChangeListener.class, "vetoableChange");
			if (this.bound)
				createSupportProperty(outline, classOutline, PropertyChangeSupport.class, PropertyChangeListener.class, "propertyChange");


			for (final JFieldVar field : definedClass.fields().values()) {
				//final JFieldVar field = definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
				final JMethod oldSetter = definedClass.getMethod("set" + outline.getModel().getNameConverter().toPropertyName(field.name()), new JType[]{field.type()});
				if (oldSetter != null && !field.type().isArray()) {
					definedClass.methods().remove(oldSetter);
					final JMethod setter = definedClass.method(setterAccess, m.VOID, "set" + outline.getModel().getNameConverter().toPropertyName(field.name()));
					final JVar setterArg = setter.param(JMod.FINAL, field.type(), "value");
					final JBlock body = setter.body();
					final JVar oldValueVar = body.decl(JMod.FINAL, field.type(), BoundPropertiesPlugin.OLD_VALUE_VAR_NAME, JExpr._this().ref(field));

					if (this.constrained) {
						final JTryBlock tryBlock;
						final JBlock block;
						if (this.setterThrows) {
							block = body;
							setter._throws(PropertyVetoException.class);
						} else {
							tryBlock = body._try();
							block = tryBlock.body();
							final JCatchBlock catchBlock = tryBlock._catch(m.ref(PropertyVetoException.class));
							final JVar exceptionVar = catchBlock.param("x");
							catchBlock.body()._throw(JExpr._new(m.ref(RuntimeException.class)).arg(exceptionVar));
						}
						invokeListener(block, field, oldValueVar, setterArg, "vetoableChange");
					}

					body.assign(JExpr._this().ref(field), setterArg);

					if (this.bound) {
						invokeListener(body, field, oldValueVar, setterArg, "propertyChange");
					}
				}
			}
		}
		return true;
	}

	private void createSupportProperty(final Outline outline,
										final ClassOutline classOutline,
										final Class<?> supportClass,
										final Class<?> listenerClass,
										final String aspectName) {
		final JCodeModel m = outline.getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;

		final String aspectNameCap = aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1);

		if (classOutline.getSuperClass() == null) { // only generate fields in topmost classes
			final JFieldVar supportField = definedClass.field(JMod.PROTECTED | JMod.FINAL | JMod.TRANSIENT, supportClass, aspectName + BoundPropertiesPlugin.SUPPORT_FIELD_SUFFIX, JExpr._new(m.ref(supportClass)).arg(JExpr._this()));
			final JMethod addMethod = definedClass.method(JMod.PUBLIC, m.VOID, "add" + aspectNameCap + "Listener");
			final JVar addParam = addMethod.param(JMod.FINAL, listenerClass, aspectName + "Listener");
			addMethod.body().invoke(JExpr._this().ref(supportField), "add" + aspectNameCap + "Listener").arg(addParam);

			final JMethod removeMethod = definedClass.method(JMod.PUBLIC, m.VOID, "remove" + aspectNameCap + "Listener");
			final JVar removeParam = removeMethod.param(JMod.FINAL, listenerClass, aspectName + "Listener");
			removeMethod.body().invoke(JExpr._this().ref(supportField), "remove" + aspectNameCap + "Listener").arg(removeParam);
		}
		final JMethod withMethod = definedClass.method(JMod.PUBLIC, definedClass, "with" + aspectNameCap + "Listener");
		final JVar withParam = withMethod.param(JMod.FINAL, listenerClass, aspectName + "Listener");
		withMethod.body().invoke("add" + aspectNameCap + "Listener").arg(withParam);
		withMethod.body()._return(JExpr._this());

		if (classOutline.getSuperClass() != null) {
			withMethod.annotate(Override.class);
		}
	}

	private JInvocation invokeListener(final JBlock block, final JFieldVar field, final JVar oldValueVar, final JVar setterArg, final String aspectName) {
		final String aspectNameCap = aspectName.substring(0, 1).toUpperCase() + aspectName.substring(1);
		final JInvocation fvcInvoke = block.invoke(JExpr._this().ref(aspectName + BoundPropertiesPlugin.SUPPORT_FIELD_SUFFIX), "fire" + aspectNameCap);
		fvcInvoke.arg(JExpr.lit(field.name()));
		fvcInvoke.arg(oldValueVar);
		fvcInvoke.arg(setterArg);
		return fvcInvoke;
	}

	private JFieldVar generateProxyField(final ClassOutline classOutline, final FieldOutline fieldOutline) {
		final JCodeModel m = classOutline.parent().getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;
		final JFieldVar collectionField = definedClass.fields().get(fieldOutline.getPropertyInfo().getName(false));
		final JClass elementType = ((JClass) collectionField.type()).getTypeParameters().get(0);
		return definedClass.field(JMod.PRIVATE | JMod.TRANSIENT, m.ref(BoundList.class).narrow(elementType), collectionField.name() + BoundPropertiesPlugin.PROXY_SUFFIX, JExpr._null());
	}

	private JMethod generateLazyProxyInitGetter(final ClassOutline classOutline, final FieldOutline fieldOutline) {
		final JCodeModel m = classOutline.parent().getCodeModel();
		final JDefinedClass definedClass = classOutline.implClass;
		final String fieldName = fieldOutline.getPropertyInfo().getName(false);
		final String getterName = "get" + fieldOutline.getPropertyInfo().getName(true);
		final JFieldVar collectionField = definedClass.fields().get(fieldName);
		final JClass elementType = ((JClass) collectionField.type()).getTypeParameters().get(0);
		final JClass proxyFieldType = m.ref(BoundList.class).narrow(elementType);
		final JFieldRef collectionFieldRef = JExpr._this().ref(collectionField);
		final JFieldRef proxyField = JExpr._this().ref(collectionField.name() + BoundPropertiesPlugin.PROXY_SUFFIX);
		final JMethod oldGetter = definedClass.getMethod(getterName, new JType[0]);
		definedClass.methods().remove(oldGetter);
		final JMethod newGetter = definedClass.method(JMod.PUBLIC, proxyFieldType, getterName);
		newGetter.body()._if(collectionFieldRef.eq(JExpr._null()))._then().assign(collectionFieldRef, JExpr._new(m.ref(ArrayList.class).narrow(elementType)));
		final JBlock ifProxyNull = newGetter.body()._if(proxyField.eq(JExpr._null()))._then();
		ifProxyNull.assign(proxyField, JExpr._new(m.ref(BoundListProxy.class).narrow(elementType)).arg(collectionFieldRef));
		newGetter.body()._return(proxyField);
		return newGetter;
	}

	public boolean isConstrained() {
		return this.constrained;
	}

	public boolean isSetterThrows() {
		return this.setterThrows;
	}
}
