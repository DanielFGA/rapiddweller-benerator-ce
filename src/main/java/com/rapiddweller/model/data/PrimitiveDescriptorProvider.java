/*
 * (c) Copyright 2006-2021 by rapiddweller GmbH & Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU General Public License.
 *
 * For redistributing this software or a derivative work under a license other
 * than the GPL-compatible Free Software License as defined by the Free
 * Software Foundation or approved by OSI, you must first obtain a commercial
 * license to this software product from rapiddweller GmbH & Volker Bergmann.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.rapiddweller.model.data;

import com.rapiddweller.script.PrimitiveType;

/**
 * Provides Benerator's built-in type descriptors.<br/><br/>
 * Created: 05.03.2008 13:57:13
 * @author Volker Bergmann
 * @since 0.5.0
 */
public class PrimitiveDescriptorProvider extends DefaultDescriptorProvider {

  public static final String NAMESPACE = "ben";

  public PrimitiveDescriptorProvider(DataModel dataModel) {
    super(NAMESPACE, dataModel);
    for (PrimitiveType type : PrimitiveType.getInstances()) {
      addTypeDescriptor(new SimpleTypeDescriptor(type.getName(), this, type.getName()));
    }
    addTypeDescriptor(new ComplexTypeDescriptor("entity", this));
  }

  public SimpleTypeDescriptor getPrimitiveTypeDescriptor(Class<?> javaType) {
    PrimitiveType type = PrimitiveType.findByJavaType(javaType);
    if (type != null) {
      return (SimpleTypeDescriptor) getTypeDescriptor(type.getName());
    }
    return null;
  }

}
