/*
 * Copyright (c) 2011 On-Site.com.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this work. If not, see <http://www.gnu.org/licenses/>.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module. An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your
 * version of the library, but you are not obligated to do so. If you do
 * not wish to do so, delete this exception statement from your version.
 */

package com.sun.tools.hat.internal.lang.jruby12;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.hat.internal.lang.Models;
import com.sun.tools.hat.internal.lang.ObjectModel;
import com.sun.tools.hat.internal.lang.jruby.JRubyHash;
import com.sun.tools.hat.internal.model.JavaObject;
import com.sun.tools.hat.internal.model.JavaThing;

class JRubyObject extends ObjectModel {
    private final JavaObject obj;
    private final ImmutableMap<String, JavaThing> properties;

    public JRubyObject(JavaObject obj) {
        this.obj = obj;
        this.properties = makeProperties(obj);
    }

    private static ImmutableMap<String, JavaThing> makeProperties(JavaObject obj) {
        final ImmutableMap.Builder<String, JavaThing> builder = ImmutableMap.builder();
        JavaObject variables = Models.getFieldObject(obj, "variables");
        if (variables != null) {
            JavaObject packedVFields = Models.getFieldObject(variables, "packedVFields");
            if (packedVFields != null) {
                getPropertiesFromPackedFields(packedVFields, builder);
            }
            List<JavaObject> packedVTable = Models.getFieldObjectArray(variables,
                    "packedVTable", JavaObject.class);
            if (packedVTable != null) {
                getPropertiesFromPackedTable(packedVTable, builder);
            }
            List<JavaObject> vTable = Models.getFieldObjectArray(variables,
                    "vTable", JavaObject.class);
            if (vTable != null) {
                JRubyHash.iterateTable(vTable, "name", "value", "next",
                        new JRubyHash.KeyValueVisitor() {
                    @Override
                    public void visit(JavaObject key, JavaObject value) {
                        builder.put(Models.getStringValue(key), value);
                    }
                });
            }
        }
        return builder.build();
    }

    private static void getPropertiesFromPackedFields(JavaObject packedVFields,
            ImmutableMap.Builder<String, JavaThing> builder) {
        for (int i = 1; ; ++i) {
            String name = Models.getFieldString(packedVFields, "name" + i);
            if (name == null) {
                return;
            }
            builder.put(name, packedVFields.getField("value" + i));
        }
    }

    private static void getPropertiesFromPackedTable(List<JavaObject> packedVTable,
            ImmutableMap.Builder<String, JavaThing> builder) {
        int midway = packedVTable.size() / 2;
        for (int i = 0; i < midway; ++i) {
            String name = Models.getStringValue(packedVTable.get(i));
            if (name == null) {
                return;
            }
            builder.put(name, packedVTable.get(i + midway));
        }
    }

    @Override
    public String getClassName() {
        JavaObject cls = getClassObject();
        String name = Models.getFieldString(cls, "classId");
        return name != null ? name : "#<Class:" + cls.getIdString() + ">";
    }

    @Override
    public JavaObject getClassObject() {
        return Models.getFieldObject(obj, "metaClass");
    }

    @Override
    public Map<String, JavaThing> getProperties() {
        return properties;
    }
}
