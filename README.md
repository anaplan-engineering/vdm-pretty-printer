# vdm-pretty-printer

This project provides a mechanism to produce a pretty-printed representation of a VDM AST.

The pretty printer works directly from the AST to produce a rendering according to a specified strategy. A number of default [rendering strategies](#rendering-strategies) are provided with the tool. 

## Status
The principal motivation for creating this pretty printer was to produce HTML renderings of VDM-SL specifications. Thus, the focus of our work so far has been supporting VDM-SL and rendering to HTML. However, testing shows that the component is able to successfully process many VDM++ examples (more work is required on VDM-RT). Similarly, a number of [rendering strategies](#rendering-strategies) are provided, but the HTML renderer has been most tested.
 
We believe that this tool will provide useful to most VDM practitioners, but there is still some work required to guarantee the renderings produced (see [Known issues](#known-issues)). We are continuing to work on improving the range of language supported and the range and quality of the renderings (e.g. more intelligent line splitting). 

## Version
This page was last updated, to correctly describe the use and behaviour of version **2.6.2** of the pretty printer.

Version numbers are currently tied to the version of Overture that the pretty printer is compiled against.

## Example usage
Full, runnable examples can be found in [src/example](src/example)

### Kotlin example
[Kotlin](https://kotlinlang.org/) is used to implement the pretty printer. It is trivial to pretty print a specification.
```kotlin
// parse file
val specFile = File(PrettyPrintExample::class.java.getResource("example.vdmsl").toURI())
val vdmsl = VDMSL()
vdmsl.parse(listOf(specFile))
vdmsl.typeCheck()

// locate the module we want to pretty print
val module = vdmsl.interpreter.modules.first()

// pretty print
val prettyPrinter = VdmPrettyPrinter()
println(prettyPrinter.prettyPrint(module))
```

### Java example
Kotlin is a JVM language, so it is equally easy to pretty print a specification using Java.
```java
// parse file
File specFile = new File(PrettyPrintJavaExample.class.getResource("example.vdmsl").toURI());
VDMSL vdmsl = new VDMSL();
vdmsl.parse(Arrays.asList(specFile));
vdmsl.typeCheck();

// locate the module we want to pretty print
AModuleModules module = vdmsl.getInterpreter().modules.get(0);

// pretty print
VdmPrettyPrinter prettyPrinter = new VdmPrettyPrinter();
System.out.println(prettyPrinter.prettyPrint(module, new PrettyPrintConfig()));
```
Note that, it is necessary to supply a config when pretty printing with Java as default arguments are not supported.

### Rendering strategies
By default the `PlainAsciiTextRenderStrategy` is used to pretty print a specification. Alternate render strategies can be passed to the `VdmPrettyPrinter` on construction. For example:
```kotlin
val prettyPrinter = VdmPrettyPrinter(renderStrategy = MathematicalUnicodeHtmlRenderStrategy())
```

The following render strategies are packaged with this project:
- `PlainAsciiTextRenderStrategy` — intended to format a specification in place 
- `MathematicalUnicodeTextRenderStrategy` — uses mathematical symbols in favour of ASCII keywords where possible to produce a UTF-8 text representation.
- `MathematicalUnicodeHtmlRenderStrategy` — as the previous, but produces a UTF-8 HTML representation

Custom strategies can be created by implementing the `IRenderStrategy` interface.  

### Sample 
Take, For example, the following snippet of VDM-SL:
```
values
    a = 1

functions
inc: nat -> nat
inc(i) == i + 1
```

The MathematicalUnicodeHtmlRenderStrategy would produce this render:

<h3>values</h3><div id='values'/>
&nbsp;&nbsp;a&nbsp;=&nbsp;1<br/>
<h3>functions</h3><div id='functions'/>
<div id='inc'/>&nbsp;&nbsp;inc:&nbsp;ℕ&nbsp;→&nbsp;ℕ<br/>
&nbsp;&nbsp;inc(i)&nbsp;⧋<br/>
&nbsp;&nbsp;&nbsp;&nbsp;i&nbsp;+&nbsp;1<br/>

## Testing
The principal means of testing this pretty printer is through a series of tests that pretty print the example specifications that are provided with Overture and then re-parse and verify that the internal representation matches that of the original. 

These tests are co-ordinated by the classes `VdmSlReparseTest`, `VdmPpReparseTest` and `VdmRtReparseTest` and the example specifications used are found in [src/test/resources](src/test/resources).

Some of these tests do not pass when run. To track which and why we have implemented a mechanism that enables failing tests to be programmatically ignored. The presence of an `ignore` file in the root of an example directory indicates that the test will be ignored. The contents of that file indicate why the example has been ignored. Some examples fail because of issues with Overture (see [Known issues](#known-issues)), some because we don't support VDM-RT yet and some simply haven't been fully investigated.

## Known issues

### Inability to disambiguate between let statement and define statement

See https://github.com/overturetool/overture/issues/683

In the StatementReader define statements are parsed as let statements (see https://github.com/overturetool/overture/blob/development/core/parser/src/main/java/org/overture/parser/syntax/StatementReader.java#L946).

This means that after parsing it is impossible to disambiguate a let statement from a define statement. This means that, for example, it is not possible to faithfully pretty print from the AST.

This causes the following test examples to fail. Each of these tests passes when run with a patched version of Overture (see https://github.com/anaplan-engineering/overture/tree/addExplicitDefineStatements)

- webserverPP
- HomeAutomationConcPP
- MSAWconcurPP
- BuslinesWithDBPP
- BuslinesPP
- ReaderWriterPP
- HomeautomationSeqPP
- MSAWseqPP
- worldcupPP
- treePP
- stackPP
- PacemakerConcPP
- expressSL
- MetroInterlockingPP
