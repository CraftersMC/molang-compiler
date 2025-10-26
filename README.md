[![Jenkins](https://img.shields.io/jenkins/build?jobUrl=https://ci.blamejared.com/job/Foundry/job/molang-compiler/job/master/&style=?style=plastic)](https://ci.blamejared.com/job/Foundry/job/molang-compiler/job/master/)

# Molang Compiler

High-speed MoLang compiler and executor designed with per-frame execution in mind. This implementation compiles MoLang
expressions to Java bytecode for optimal runtime performance.

## MoLang Compliance

See [MOLANG_COMPLIANCE.md](MOLANG_COMPLIANCE.md) for detailed compatibility information.

For official MoLang documentation, see:

- [Microsoft Learn - MoLang Reference](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/)
- [Microsoft Learn - MoLang Syntax Guide](https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/syntax-guide)

# How to add to your workspace

There are two main ways to use this in your application. If you are writing a Minecraft Mod with NeoForge or Fabric,
install [Veil](https://github.com/FoundryMC/Veil) which already has the library shadowed. If you don't want
to add another library, you can just manually shadow this library into your mod.

```gradle
plugins {
    id 'com.github.johnrengelman.shadow' version "8.1.1"
}

configurations {
    shade
}

repositories {
    maven {
        name = "Jared's maven"
        url = "https://maven.blamejared.com/"
    }
}

dependencies {
    implementation "gg.moonflower:molang-compiler:version"
    shade "gg.moonflower:molang-compiler:version"
}

shadowJar {
    configurations = [project.configurations.shade]
    relocate 'gg.moonflower.molangcompiler', 'your.project.lib.molangcompiler'
}
```

# Usage

When using this library with a regular java program, you can use GlobalMolangCompiler to retrieve new instances of the
standard compiler. The compiler is designed this way to allow the garbage collector to delete old expressions if they
aren't needed anymore.

For example

```java
public class Main {

    public static void main(String[] args) {
        MolangCompiler compiler = GlobalMolangCompiler.get();
    }
}
```

When in an environment like NeoForge or Fabric a custom molang compiler instance must be created as a child of the mod
class loader. If using Veil, this step is already handled.

```java

@Mod("modid")
public class NeoForgeMod {

    public NeoForgeMod() {
        MolangCompiler compiler = MolangCompiler.create(MolangCompiler.DEFAULT_FLAGS, NeoForgeMod.class.getClassLoader());
    }
}
```

## Java Integration

- **Java function calls**: Execute custom Java functions from MoLang expressions
  ```molang
  custom_lib.javaFunction(param1, param2, param3)
  ```
- **Custom libraries**: Register your own MoLang libraries with custom functions

**Note:** When writing MoLang expressions that need to be compatible with Minecraft Bedrock Edition, avoid using these
extensions.

# Examples

Compiling and using expressions:

```java
public class Example {

    private final MolangExpression speed;
    private final MolangExpression time;

    // MolangEnvironment#resolve(MolangExpression) allows the caller to handle errors created while resolving
    // The most common reason for an error is a variable being used that isn't defined in the environment
    public float getSpeed(MolangEnvironment environment) throws MolangRuntimeException {
        return environment.resolve(this.speed);
    }

    // Alternatively MolangEnvironment#safeResolve(MolangExpression) can be used to print the stack trace and return 0 on errors
    public float getTime(MolangEnvironment environment) {
        return environment.safeResolve(this.time);
    }

    public static @Nullable Example deserialize(String speedInput, String timeInput) {
        try {
            // Note: this cannot be used in a modded environment.
            // The compiler used should be a global instance
            // created like the NeoForgeMod example
            MolangCompiler compiler = GlobalMolangCompiler.get();

            // Expressions can be compiled from a valid MoLang string
            // Note that compilation is relatively expensive(~15ms sometimes) so it should be done once and cached
            MolangExpression speed = compiler.compile(speedInput);
            MolangExpression time = compiler.compile(timeInput);

            return new Example(speed, time);
        } catch (MolangSyntaxException e) {
            // The exception gives a message similar to Minecraft commands
            // indicating exactly what token was invalid
            e.printStackTrace();
            return null;
        }
    }
}
```

Using variables:

```java
public class Foo {

    public void run() {
        // A runtime is the base implementation of an environment.
        // The provided builder can add variables, queries, globals, and extra libraries
        MolangEnvironment environment = MolangRuntime.runtime()
                .setQuery("foo", 4)
                .setQuery("bar", 12)
                .create();

        Example example = Example.deserialize("(q.foo - q.bar) > 0", "q.foo * q.bar");
        // The environment will use the values specified in the builder as replacements when calculating the expression
        // In this example, the result will become 4 * 12 = 48
        float time = example.getTime(environment);

        // See the documentation for more details on adding java functions and variables
    }
}
```

Adding Custom MoLang Libraries

```java
public class BarLibrary extends MolangLibrary {

    @Override
    protected void populate(BiConsumer<String, MolangExpression> consumer) {
        // Add all expressions that should be registered under "libname"
        // For example, this becomes "libname.secret" in MoLang code
        consumer.accept("secret", MolangExpression.of(42));
    }

    // This name is used for printing and identification.
    // The actual namespace of the library is defined when adding it to a MolangEnvironment.
    @Override
    protected String getName() {
        return "libname";
    }

    public static void addLibraries(MolangEnvironment environment) {
        // Environments can be immutable and will throw an exception if they are tried to be modified
        if (!environment.canEdit()) {
            throw new UnsupportedOperationException("Environment is immutable");
        }

        MolangEnvironmentBuilder<? extends MolangEnvironment> builder = environment.edit();
        builder.setQuery("loadedBar", 1);

        // This will allow MoLang expressions to resolve "libname.secret" now
        environment.loadLibrary("libname", new BarLibrary());
    }
}
```

# Credit

Buddy for writing the Java bytecode generation and class loader. https://twitter.com/BuddyYuz
