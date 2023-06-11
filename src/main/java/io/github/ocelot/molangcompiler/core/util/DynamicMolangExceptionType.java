// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package io.github.ocelot.molangcompiler.core.util;

import io.github.ocelot.molangcompiler.api.exception.MolangSyntaxException;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * Modified version of DynamicCommandExceptionType from <a href=https://github.com/Mojang/brigadier/blob/master/src/main/java/com/mojang/brigadier/exceptions/DynamicCommandExceptionType.java>Brigadier</a>
 */
@ApiStatus.Internal
public class DynamicMolangExceptionType {

    private final Function<Object, String> function;

    public DynamicMolangExceptionType(Function<Object, String> function) {
        this.function = function;
    }

    public MolangSyntaxException create(Object arg) {
        return new MolangSyntaxException(this.function.apply(arg));
    }

    public MolangSyntaxException createWithContext(StringReader reader, Object arg) {
        return new MolangSyntaxException(this.function.apply(arg), reader.getString(), reader.getCursor());
    }
}