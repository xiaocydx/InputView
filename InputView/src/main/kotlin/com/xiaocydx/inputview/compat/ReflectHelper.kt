/*
 * Copyright 2023 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.inputview.compat

import android.os.Build
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * 反射帮助类
 *
 * @author xcc
 * @date 2023/2/2
 */
internal interface ReflectHelper {

    val Class<*>.declaredStaticFields: List<Field>
        get() {
            val fields = runCatching {
                if (Build.VERSION.SDK_INT < 28) {
                    declaredFields.filter { Modifier.isStatic(it.modifiers) }
                } else {
                    HiddenApiBypass.getStaticFields(this)
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (fields.getOrNull() ?: emptyList<Field>()) as List<Field>
        }

    val Class<*>.declaredInstanceFields: List<Field>
        get() {
            val fields = runCatching {
                if (Build.VERSION.SDK_INT < 28) {
                    declaredFields.filter { !Modifier.isStatic(it.modifiers) }
                } else {
                    HiddenApiBypass.getInstanceFields(this)
                }
            }
            @Suppress("UNCHECKED_CAST")
            return (fields.getOrNull() ?: emptyList<Field>()) as List<Field>
        }

    fun Field.toCache() = FieldCache(this)

    fun Constructor<*>.toCache() = ConstructorCache(this)

    fun List<Field>.find(name: String): Field = first { it.name == name }

    fun List<Field>.findOrNull(name: String): Field? = find { it.name == name }
}

internal class FieldCache(private val field: Field) {
    init {
        field.isAccessible = true
    }

    fun get(obj: Any?): Any? {
        return runCatching { field.get(obj) }.getOrNull()
    }

    fun set(obj: Any?, value: Any?): Boolean {
        return runCatching { field.set(obj, value) }.isSuccess
    }
}

internal class ConstructorCache(private val constructor: Constructor<*>) {
    init {
        constructor.isAccessible = true
    }

    fun newInstance(vararg initargs: Any): Any? {
        return runCatching { constructor.newInstance(*initargs) }.getOrNull()
    }
}