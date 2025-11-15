# MoLang Compliance Documentation

This document describes how this compiler implementation aligns with the official Mojang MoLang specification.

## Official MoLang Reference

- [Microsoft Learn - MoLang Reference](https://learn.microsoft.com/en-us/minecraft/creator/reference/content/molangreference/)
- [Microsoft Learn - MoLang Syntax Guide](https://learn.microsoft.com/en-us/minecraft/creator/documents/molang/syntax-guide)
- [bedrock.dev - MoLang Documentation](https://bedrock.dev/docs/stable/Molang)

## Supported Operators

### Fully Implemented

- **Logical:** `!`, `||`, `&&`
- **Comparison:** `<`, `<=`, `>=`, `>`, `==`, `!=`
- **Arithmetic:** `*`, `/`, `+`, `-`
- **Ternary Conditional:** `? :`
- **Null Coalescing:** `??`
- **Assignment:** `=`, `+=`, `-=`, `*=`, `/=`
- **Increment/Decrement:** `++`, `--`

### Not Implemented

- **Arrow Operator:** `->` (accesses data from different entities)

## Variable Scopes

### Fully Implemented

All official MoLang variable scopes are supported:

| Scope        | Aliases | Lifetime         | Access     |
|--------------|---------|------------------|------------|
| `temp.*`     | `t.*`   | Expression scope | Read/Write |
| `variable.*` | `v.*`   | Entity lifetime  | Read/Write |
| `query.*`    | `q.*`   | Read-only        | Read       |
| `context.*`  | `c.*`   | Expression scope | Read       |
| `global.*`   | -       | Persistent       | Read/Write |

## Control Flow

### Fully Implemented

- `loop(count, expression)` - Executes code repeatedly
- `break` - Exit loops early
- `continue` - Skip to next loop iteration
- `return` - Explicitly return value
- `if(condition) expression` - Conditional execution
- `condition ? expression` - Conditional execution

### Limitations

- Maximum loop iterations: Not enforced (official spec: 1024)
- `break` and `continue` are validated at compile-time to be inside loops
- For each operations (`for_each(<variable>, <array>, <expression>)`) are not supported

## Math Functions

### Official MoLang Functions Implemented

All official MoLang math functions are supported:

| Function                                | Parameters | Description                       |
|-----------------------------------------|------------|-----------------------------------|
| `math.abs(value)`                       | 1          | Absolute value                    |
| `math.acos(value)`                      | 1          | Inverse cosine (returns degrees)  |
| `math.asin(value)`                      | 1          | Inverse sine (returns degrees)    |
| `math.atan(value)`                      | 1          | Inverse tangent (returns degrees) |
| `math.atan2(y, x)`                      | 2          | Arctan of y/x (returns degrees)   |
| `math.ceil(value)`                      | 1          | Round up to nearest integer       |
| `math.clamp(value, min, max)`           | 3          | Clamp value between min and max   |
| `math.cos(value)`                       | 1          | Cosine (input in degrees)         |
| `math.sin(value)`                       | 1          | Sine (input in degrees)           |
| `math.die_roll(num, low, high)`         | 3          | Sum of random floats              |
| `math.die_roll_integer(num, low, high)` | 3          | Sum of random integers            |
| `math.exp(value)`                       | 1          | e to the power of value           |
| `math.floor(value)`                     | 1          | Round down to nearest integer     |
| `math.hermite_blend(value)`             | 1          | Smooth interpolation (3t² - 2t³)  |
| `math.lerp(start, end, t)`              | 3          | Linear interpolation              |
| `math.lerprotate(start, end, t)`        | 3          | Rotation interpolation            |
| `math.ln(value)`                        | 1          | Natural logarithm                 |
| `math.max(a, b)`                        | 2          | Maximum of two values             |
| `math.min(a, b)`                        | 2          | Minimum of two values             |
| `math.min_angle(value)`                 | 1          | Minimize angle to [-180, 180)     |
| `math.mod(value, denominator)`          | 2          | Modulo operation                  |
| `math.pi`                               | 0          | Returns π (constant)              |
| `math.pow(base, exponent)`              | 2          | Exponentiation                    |
| `math.random(low, high)`                | 2          | Random float in range             |
| `math.random_integer(low, high)`        | 2          | Random integer in range           |
| `math.round(value)`                     | 1          | Round to nearest integer          |
| `math.sqrt(value)`                      | 1          | Square root                       |
| `math.trunc(value)`                     | 1          | Round toward zero                 |

### Compiler Extensions (Not in Official MoLang)

These functions are **extensions** provided by this compiler and are **not** part of the official MoLang specification:

| Function                            | Parameters | Description                       |
|-------------------------------------|------------|-----------------------------------|
| `math.sign(value)`                  | 1          | Returns -1, 0, or 1 based on sign |
| `math.triangle_wave(x, wavelength)` | 2          | Triangle wave oscillation         |

## Data Types

### Implemented

- **Float** - All numerical values (primary type)
- **Boolean** - Converted to `1.0` (true) or `0.0` (false)
- **String** - Single-quoted strings (limited support)

### Partial or Not Implemented

- **Structs** - Limited, although there is support for variables with multiple parts. Eg: `v.pos.x = 4`

### Known Differences

- String escape sequences: Official MoLang has no escape character support; this compiler supports basic escapes
- Double negation `--5`: Parsed as decrement operator rather than double negative
- Multiple return statements: Official behavior unclear; this compiler may handle differently

### For Contributors

1. **Reference official docs** when implementing new features
2. **Align test expectations** with official MoLang behavior where possible
3. **Document deviations** from the spec in this file

## Compiler Versioning

This compiler supports versioned compilation. This allows the compiler to maintain backward
compatibility with different MoLang specifications while adding new features.

### Current Versions

| Version | Description                                             | Status      |
|---------|---------------------------------------------------------|-------------|
| 12      | Latest implementation, supports all documented features | Implemented |

### Version-Specific Behavior

Different compiler versions may have subtle differences in:

- **Parsing rules** - How expressions are tokenized and parsed
- **Operator precedence** - Order of operations for complex expressions
- **Available functions** - Which built-in functions are available
- **Error handling** - How syntax errors are reported

When compiling expressions, you can specify a target version:

```java
MolangCompiler compiler = GlobalMolangCompiler.get();
MolangExpression expr = compiler.compile("q.foo * 2", MolangVersion.get(12));
```

If no version is specified, `MolangVersion.LATEST` (currently v12) is used.

## Version Information

- **Compiler Version:** 4.0.0
- **Default MoLang Version:** 12
- **MoLang Spec Reference:** Minecraft Bedrock Edition (latest stable)
- **Last Updated:** 2025-11-15
