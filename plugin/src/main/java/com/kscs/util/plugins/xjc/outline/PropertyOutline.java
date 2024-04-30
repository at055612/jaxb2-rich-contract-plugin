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

package com.kscs.util.plugins.xjc.outline;

import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.glassfish.jaxb.core.v2.model.core.TypeInfo;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JType;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.model.nav.NType;

/**
 * @author Mirko Klemm 2015-01-28
 */
public interface PropertyOutline {
	String getBaseName();
	String getFieldName();
	JType getRawType();
	JType getElementType();
	JFieldVar getFieldVar();
	boolean hasGetter();
	boolean isCollection();
	boolean isIndirect();
	List<TagRef> getChoiceProperties();
	JClass getMutableListClass();

	/**
	 * @return The annotation description text from the corresponding part of the schema if there is any.
	 */
	default Optional<String> getSchemaAnnotationText() {
		return Optional.empty();
	}

	/**
     * @param tagRef The {@link TagRef} of the referenced type to get the schema annotation for
	 * @return The annotation description text from the corresponding part of the schema if there is any.
	 */
	default Optional<String> getSchemaAnnotationText(TagRef tagRef) {
		return Optional.empty();
	}

	JDefinedClass getReferencedModelClass();

	class TagRef {
		private final QName tagName;
		private final TypeInfo<NType,NClass> typeInfo;

		public TagRef(final QName tagName, final TypeInfo<NType, NClass> typeInfo) {
			this.tagName = tagName;
			this.typeInfo = typeInfo;
		}

		public QName getTagName() {
			return this.tagName;
		}

		public TypeInfo<NType, NClass> getTypeInfo() {
			return this.typeInfo;
		}
	}
}
