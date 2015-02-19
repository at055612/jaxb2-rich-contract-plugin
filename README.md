jaxb2-rich-contract-plugin
==========================

<span style="color: red; font-weight: bold">Note: The information in this file may be outdated, refer to (http://mklemm.github.io/jaxb2-rich-contract-plugin) for the most recent documentation!</span>

Current Version: 1.5.1

Get it with Maven:

Repository-URI: http://maven.klemm-scs.com/release

```xml
<groupId>com.kscs.util</groupId>
	<artifactId>jaxb2-rich-contract-plugin</artifactId>
<version>1.5.1</version>
```
## Overview
This module is a collection of several plugins for the JAXB2 (Java API for XML binding) "XSD to Java Compiler" (XJC).
These plugins are intended to add support for additional contracts to the classes generated by XJC.
Currently, there are 5 plugin classes:

1. **[group-interface](#group-interface)**: When using `<attribute-group>` or `<group>` elements in an XSD, they are transformed as interface
   definitions, and any complexTypes using the groups will be generated as classes implementing this interface.
2. **[constrained-properties](#constrained-properties)**: Will generate a complexTypes element members as bound and/or constrained properties as per
   the JavaBeans spec.
3. **[clone](#clone)**: Will generate a simple deep "clone" method for the generated classes based on the heuristic that it only
   makes sense to traverse further down in the cloned object tree for members of types that are actually cloenable themselves.
4. **[copy](#copy)**: Similar to "clone", will generate a simple deep "createCopy" method. The java API contract for the `java.lang.Cloneable`
	interface and the rules for overriding `Object.clone()` are defective by design. So the "copy" plugin uses its own
	API to realize the desired behavior.
	Also can generate a "partial createCopy" method, that takes a `PropertyTree` object which represents an include/exclude
	rule for nodes in the object tree to clone. Excluded nodes will not be cloned and left alone.
	Optionally, corresponding copy constructors can also be generated.
4. **[immutable](#immutable)**: Will make generated classes immutable. Only makes sense together with "fluent-builder" plugin (see below),
	or any other builder or initialisation facility, like the well-known "value-constructor" plugin.
5. **[fluent-builder](#fluent-builder)**: Generates a builder class for every class generated. Builders are implemented as inner classes,
	static methods are provided for a fluent builder pattern in the form `MyClass.builder().withPropertyA(...).withPropertyB(...).build()`.
	Builders also contain "copy..." methods to initialize the builder from another instance. Partial copying is also supported in the same way as in **clone**.
	This is particularly useful together with `-Ximmutable` (see above), but not usable together with `-Xconstrained-properties` (see below).
6. **[meta](#meta)**: Generates a nested class representing a static metamodel of the generated classes. In the "enhanced" version, this contains
	information about the type and the XSD element from which the property was generated, in "simple" mode, there are only constants for the property names.



### Usage
#### General
jaxb2-rich-contract-plugin is a plugin to the XJC "XML to Java compiler" shipped with the
reference implementation of JAXB, included in all JDKs since 1.6.
It is targeted on version 2.2 of the JAXB API.
In order to make it work, you need to:

* Add the jar file to the classpath of XJC

* Add the JAXB 2.2 XJC API to the classpath of XJC, if your environment is running by default under JAXB 2.1 or lower.

* Add the corresponding activating command-line option to XJC's invocation,
  see below for details of each of the plugins

* Each of the plugins, except "-Ximmutable", has one or more sub-options to fine-control its behavior.
  These sub-option must be given after the corresponding main "-X..." activation option, to avoid naming conflicts.
  Names of sub-options can be given dash-separated or in camelCase.

* The "immutable" and "constrained-properties" plugins are mutually exclusive. An object cannot be both immutable
  and send change notifications.

#### From Maven
There is a maven repository for this project under:

http://maven.klemm-scs.com/release

Add this repository to your pom.xml:

	<repositories>
		<repository>
			<releases>
		        <enabled>true</enabled>
		        <updatePolicy>always</updatePolicy>
		        <checksumPolicy>warn</checksumPolicy>
		    </releases>
			<id>jaxb2-plugins</id>
			<name>JAXB2 XJC Plugin Repository</name>
			<url>http://maven.klemm-scs.com/release</url>
			<layout>default</layout>
		</repository>
	</repositories>
	


You should add "maven-jaxb2-plugin" to your `<build>`
configuration.
Then add "jaxb2-rich-contract-plugin" as an XJC plugin ("plugin for plugin") to the maven plugin declaration.
The following example shows all possible options reflecting their default values:
````xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.jvnet.jaxb2.maven2</groupId>
                <artifactId>maven-jaxb2-plugin</artifactId>
                <version>0.11.0</version>
                <executions>
                    <execution>
                        <id>xsd-generate-2.1</id>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>generate</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <schemaIncludes>
                        <schemaInclude>**/*.xsd</schemaInclude>
                    </schemaIncludes>
                    <strict>true</strict>
                    <verbose>true</verbose>
                    <extension>true</extension>
                    <removeOldOutput>true</removeOldOutput>
                    <args>
						<arg>-Xclone</arg>
							<arg>-cloneThrows=y</arg>
						<arg>-Xconstrained-properties</arg>
							<arg>-constrained=y</arg>
							<arg>-bound=y</arg>
							<arg>-setterThrows=n</arg>
							<arg>-generateTools=y</arg>
						<arg>-Xfluent-builder</arg>
							<arg>-generateTools=y</arg>
							<arg>-narrow=n</arg>
							<arg>-copyPartial=y</arg>
							<arg>-selectorClassName=Selector</arg>
							<arg>-rootSelectorClassName=Select</arg>
							<arg>-builderClassName=Builder</arg>
							<arg>-newBuilderMethodName=builder</arg>
						<arg>-Xcopy</arg>
							<arg>-partial=y</arg>
							<arg>-generateTools=y</arg>
							<arg>-constructor=y</arg>
							<arg>-narrow=n</arg>
							<arg>-selectorClassName=Selector</arg>
							<arg>-rootSelectorClassName=Select</arg>
						<arg>-Xgroup-contract</arg>
							<arg>-declareSetters=y</arg>
							<arg>-declareBuilderInterface=y</arg>
							<arg>-upstreamEpisodeFile=/META-INF/jaxb-interfaces.episode</arg>
							<arg>-downstreamEpisodeFile=/META-INF/jaxb-interfaces.episode</arg>
						<arg>-Xmeta</arg>
							<arg>-generateTools=y</arg>
							<arg>-extended=n</arg>
							<arg>-camelCase=n</arg>
							<arg>-metaClassName=PropInfo</arg>
						<arg>-Ximmutable</arg>
                        <arg>...</arg>
                    </args>
                    <plugins>
                        <plugin>
                            <groupId>com.kscs.util</groupId>
                            <artifactId>jaxb2-rich-contract-plugin</artifactId>
                            <version>1.5.1</version>
                        </plugin>
                    </plugins>
                    <dependencies>
                        <!-- Put this in if your default JAXB version is 2.1 or lower,
                        or if &quot;tools.jar&quot; isn't in your classpath -->
						<dependency>
							<groupId>org.glassfish.jaxb</groupId>
							<artifactId>jaxb-runtime</artifactId>
							<version>2.2.11</version>
						</dependency>
						<dependency>
							<groupId>org.glassfish.jaxb</groupId>
							<artifactId>jaxb-core</artifactId>
							<version>2.2.11</version>
						</dependency>
						<dependency>
							<groupId>org.glassfish.jaxb</groupId>
							<artifactId>jaxb-xjc</artifactId>
							<version>2.2.11</version>
						</dependency>
	                </dependencies>
                </configuration>
            </plugin>
        </plugins>
    </build>
```
Note: the `<extension>` flag must be set to "true" in order to make XJC accept any extensions at all.

Note: jaxb2-rich-contract-plugin implements JAXB and XJC APIs version 2.2.7. You most likely will have
	to add the dependencies to these libraries to your classpath effective at XJC runtime. See the
	`dependencies` element above on how to do this.


### Version History
* **1.0.0**:	Initial Version
* **1.0.1**:	Added constrained-property plugin
* **1.0.2**:	Added partial clone method generation
* **1.0.3**:	Improvements in partial clone
* **1.0.4**:	Added fluent builder and immutable plugins
* **1.0.5**:    Added chainable fluent builder support
* **1.1.0**:    New: `-Ximmutable`, Copy constructor support,
				fluent-builder copy from instance support,
				general fixes.
				Removed option to generate fluent builders without
				chained builder support.
* **1.1.1**:	New: Type-safe selector support for partial clone/copy logic.
* **1.1.2**:	Big fixes in selector logic
* **1.1.3**:	Minor bug fixes in fluent-builder
* **1.1.4**:	Fixed an error in fluent-builder where an initialization method wasn't properly overridden in derived builder classes, leading to the wrong builder type being returned when using chained sub-builders.
* **1.1.5**:    Fixed error in Release Build process
* **1.1.6**:	Fixed bug in group-contract plugin: Property names customised via binding info were generated incorrectly in the interface definitions.
* **1.2.0**:    Major changes to the logic of partial cloning. The partial clone `PropertyTree` pattern replaces the previous `PropertyPath`, which had pretty
				unclear semantics. The new `PropertyTree` builders now just create a property tree, and on invocation of the "clone()" or "copyOf()" methods or the
				copy constructor, it is decided by an additional parameter whether the property tree should be considered an exclusion or an inclusion pattern.
				Additionally, the group-interface plugin has been modified to create interfaces also for the fluent builders, if the fluent-builder plugin is activated.
* **1.2.3**:	Added "Copyable" interface and "createCopy" method which does the same thing as the "clone()" method, but doesn't suffer from the defective-by-design java.lang.Cloneable contract. It is planned to als add a "copyFrom" method to copy the state of another object into an existing object.
* **1.3.1**:    Made fluent-builder plugin work in an "episode" (modular generation and compilation) context by also integrating compiled classes on the XJC classpath in the search for base and property classes.
* **1.3.6**:    Also made group-interface work in an "episode" context, and fixed bug where empty interfaces were created if no implementation class for them could be found in the current module.
* **1.4.0**:    group-interface is using its own episode file to maintain relationships to definitions in upstream modules.
				Command-line options for a specific plugin must now be given immediately after the plugin activation option ("-X..."). This way, name conflicts between plugin options are avoided.
				Static source files are generated via the JCodeModel.addResourceFile API, so a bug where the source files ended up in the root of the project tree should be fixed now.
				group-interface and fluent-builder now are working together more reliably.
* **1.5.0**:    Added new Plugin "-Xmeta" to generate an inner class containing static meta information about the properties of a class.
				Internally, a common base class for plugins was extracted to help in command-line parsing and command-line documentation.
* **1.5.1**:    Major updates to documentation, improvements to `-Xmeta` to expose static information about XSD definitions of properties.


## group-interface

### Motivation
Out of the box, the only polymorphism supported by classes generated from an XSD is the `<extension>` notion,
transformed directly into an inheritance relationship by XJC.
However, pure inheritance relationships are often inflexible and do not always reflect the intention
of generating a "contract" that implementing classes must follow.

With this plugin, it is possible for your application code to treat classes using one or more specific model or attribute groups in a common way. Objects can be initialised or used via the interface.

### Function
For definition of contracts, two additional XSD constructs, the `<group>` and `<attributeGroup>`,
are readily available in XSD, but they're currently ignored by standard XJC and simply treated as an inclusion of
elements or attributes into a generated class definition.
The group-interface plugin changes that and generates an `interface` definition for each `group` or `attributeGroup`
found in your model, defines the attributes or elements declared in the groups as get and set methods on the interface,
and makes each generated class using the group or attributeGroup implement this interface.
New in version 1.2: The group-interface plugin will generate interfaces for the generated fluent builder classes,
if the fluent-builder plugin is also activated.

### Usage
See below on how to add the jaxb2-rich-contract-plugin to your plugin path when building your project with maven.
group-interface is activated by adding the `-Xgroup-contract` command-line option to your XJC invocation.
For group-interface, there are currently no further command line options.

### Bugs
* Currently none known.



## constrained-properties

### Motivation
Many GUI applications use data binding to connect the data model to the view components. The JavaBeans standard
defines a simple component model that also supports properties which send notifications whenever the are about to be changed,
and there are even vetoable changes that allow a change listener to inhibit modification of a property.
While the JAvaBeans standard is a bit dated, data binding and property change notification can come in handy in many situations,
even for debugging or reverse-engineering existing code, because you can track any change made to the model instance.

### Function
constrained-properties generates additional code in the property setter methods of the POJOs generated by XJC that
allow `PropertyChangeListener`s and `VetoableChangeListener`s to be attached to any instance of a XJC-generated class.

Currently, **indexed properties** are NOT supported in the way specified by JavaBeans, but instead, if a property represents a collection,
a collection proxy class is generated that supports its own set of collection-specific change notifications, vetoable and other.
This decision has been made because by default XJC generates collection properties rather than indexed properties,
and indexed properties as mandated by JavaBeans are generally considered "out of style".

### Usage
Activate the plugin by giving command-line option `-Xconstrained-properties` to XJC.

Other options supported:

#### `-setter-throws=`y/n
Generate the setter method with "throws PropertyVetoException" if constrained properties are
used. If no, only a RuntimeException is thrown on a PropertyVeto event. Default: n
							
#### `-constrained=`y/n
Generate constrained properties, where a listener can inhibit the property change. Default: y

#### `-bound=`y/n
Generate bound properties. Default: y

#### `-generate-tools=`y/n
To support Collection-specific change events and behavior, additional classes are required.
If you set this option to "yes", these auxiliary classes will be generated into the source
code along with the generated JAXB classes. If you set this to "no", you will have to include
the plugin artifact into the runtime classpath of your application.

### Limitations
* The JavaBeans standard is only loosely implemented in the generated classes.

* Indexed Properties as defined in JavaBeans are not supported.

* The CollectionChange behavior implemented by the classes is not yet documented
  and non-standard.

### Bugs
* Currently none known.


## copy

### Motivation
Sometimes it is necessary to create a deep copy of an object. There are various approaches to this. The "copy" plugin
defines its own interface, contract, and definitions that are somewhat different from the standard java "java.lang.Cloneable"
contract. The entry point generated in the source code is called `createCopy`, there are optionally also copy constructors.

### Function
The `copy` plugin generates a deep clone method for each of the generated classes, based on the following assumptions:
* Instances of any other classes implementing the `com.kscs.util.jaxb.Copyable` interface are copyable by the same
  semantics as "this".

* Objects implementing `java.lang.Cloneable` and not throwing "CloneNotSupportedException" are also reliably cloneable
  by their "clone" Method.

* Objects not implementing `java.lang.Cloneable` or primitive types are assumed to be immutable,
  their references are copied over, they are not cloned.

* Optionally, generates a "partial createCopy" method that takes a `PropertyTree` instance which represents a
  specification of the nodes in the object tree to copy. The PropertyTree is built up by an intuitive builder
  pattern:

`final PropertyTree excludeEmployees = PropertyTree.builder().with("company").with("employees").build();`

* There is also a type-safe way to build a PropertyPath instance by using a generated classes' `Selector` sub structure. The following will generate the same selection as above:

`final PropertyTree excludeEmployees = Business.Select.root().company().employees().build()`



Then, you would partially clone an object tree like this:
```java
    final BusinessPartner businessPartnerCopy = businessPartner.createCopy(excludeEmployees, PropertyTreeUse.EXCLUDE);
```
Which is the same as
```java
	final BusinessPartner businessPartnerCopy = businessPartner.copyExcept(excludeEmployees);
```
This way, the copy of the original `businessPartner` will have no employees attached to the contained `company`.
It is also possible to copy only a specific subset of the original object tree, excluding everything else. The
inverse result of the above would be generated by:
```java
	final BusinessPartner businessPartnerCopy = businessPartner.createCopy(excludeEmployees, PropertyTreeUse.INCLUDE);
```
or
```java
	final BusinessPartner businessPartnerCopy = businessPartner.copyOnly(excludeEmployees);
```

which will result in a businessPartnerCopy where every property is set to null, except the company property,
and in the attached company object, every property is null except "employees".
	
This works for single and multi-valued properties, where for multi-valued properties, the property tree applies to
all elements of the list of values in the same way. As of yet, there is no way to make a tree apply only to specific indexes
in generated lists.


### Usage
Plugin activation: `-Xcopy`.
Options:
#### `-constructor=`y/n
Generates a copy constructor.

#### `-narrow=`y/n
If copy constructors are created, they are generated in such a way that, in order to copy descendant objects,
they call the copy constructors of any descendant property types found in the tree. This will result in an object
tree that contains only the "narrowest" possible representation. For properties declared with an
abstract type, their `clone()`-Methods will be called instead, resulting in an exact runtime copy. Default: no.

#### `-partial=`y/n
Create partial clone method and - if constructor generation is selected, a partial copy constructor. (see above)

#### `-generateTools=`y/n
Generate prerequisite classes like e.g. `com.kscs.util.jaxb.Copyable` and `com.kscs.util.jaxb.PropertyPath` as source files into the generated source
packages. If you say 'no' here, you will have to add the jaxb2-rich-contract-plugin jar to your
compile- and runtime classpath.

### Limitations
* The `-narrow` option is a somewhat special use case and should be used carefully.


## clone

### Motivation
Another way to create a deep copy of an object tree. This adheres to the `java.lang.Cloneable` contract, but isn't
as versatile as `-Xcopy`.

### Function
The `clone` plugin generates a deep clone method for each of the generated classes, based on the following assumptions:
* Instances of any other class generated from the same XSD model are cloneable by the same semantics as "this".

* Objects implementing `java.lang.Cloneable` and not throwing "CloneNotSupportedException" are also reliably cloneable
  by their "clone" Method.

* Objects not implementing `java.lang.Cloneable` or primitive types are assumed to be immutable,
  their references are copied over, they are not cloned.

### Usage
Plugin activation: `-Xclone`.
Options:

#### `-cloneThrows=`y/n
Declare "clone()"-Method to throw "CloneNotSupportedException" any of the cloneable child
objects have a "clone" method that declares `CloneNotSupportedException` being thrown.
The JDK spec says objects should declare their "clone" method with "throws CloneNotSuppoertedException"
in order to enable subclasses to inhibit cloning even if their superclass declares "Cloneable".
In pratice, however, this doesn't make much sense and is against object-oriented principles.
All classes implementing `Cloneable` should really be cloneable and NOT throw
a `CloneNotSupportedException`.
Ordinary Exceptions during cloning are rethrown as `RuntimeExceptions`,
if this is set to "no", this is also true for any exceptions thrown by descendant objects,
no matter whether they are `CloneNotSupported`or anything else. If this is "yes", any possible
`CloneNotSupportedException` will be declared in the throws clause. This option will not
affect generated "partial" clone methods. Default: no.


## immutable

### Motivation
Contemporary programming styles include making objects immutable as much as possible, to minimise
side effects and allow for functional programming patterns.

### Function
This plugin simply makes all "setXXX" methods "protected", thus preventing API consumers to modify
state of instances of generated classes after they have been created. This only makes sense together with
another plugin that allows for initialization of the instances, like e.g. the included `fluent-builder` plugin.
For collection-valued properties, `-Ximmutable` wraps all collections in a `Collections.unmodifiableCollection`,
so collections are also made immutable.
Because JAXB serialization has a number of constraints regarding the internals of JAXB serializable objects,
it wasn't advisable to just remove the setter methods or replace the collections with unmodifiable collections.
So, a bit of additional code will be created that leaves the old "mutable" structure of the class intact as much
as is needed for JAXB, but modifies the public interface so objects appear immutable to client code.

### Usage
Plugin activation: `-Ximmutable`

### Limitations
* Access level "protected" may not be strict enough to prevent state changes.
* If you activate plugins like "fluent-api" or the like, these plugins may circumvent the protection provided by the `immutable` plugin.


## fluent-builder

### Motivation
There already is the widely used "fluent-api" plugin for XJC.
That, however isn't a real builder pattern since there is no strict programmatic distinction between initialization
and state change in fluent-api.

fluent-builder now creates a real "Builder" pattern, implemented as an inner class to the generated classes.

### Function
fluent-builder creates a static inner class for every generated class representing the builder, and a static
method on the generated class to create a builder.

If the "immutable" plugin is also activated, publicly exposed collections will be immutable, too.

Example use in code:
```java
	MyElement newElement = MyElement.builder().withPropertyA(...).withPropertyB(...).addCollectionPropertyA(...).build();
```
In addition, new instances can be created as copies of existing instances using the builder, with an optional modification by othe builder methods:
```java
	MyElement newElement = MyElement.copyOf(oldElement).withPropertyA(...).withPropertyB(...).build();
```	
The "partial" copy introduced in the "clone" plugin will work here as well:
```java
	PropertyTree selection = MyElement.Select.root().propertyA().propertyAB().build();
	MyElement newElement = MyElement.copyExcept(oldElement, selection).withPropertyA(...).withPropertyB(...).build();
```
Often, properties of generated classes represent containment or references to generated classes in the same model.
The fluent-builder plugin lets you initialise properties of such a type - if it isn't an abstract type - by using sub-builders ("chained" builders) in the following way, given that both A and B are types defined in the XSD model, and A has a property of type B, and B has three properties of type String, x,y, and z:
```java
	A newA = A.builder().withB().withX("x").withY("y").withZ("z").end().build();
```
Of course, this plugin is most useful if `immutable` is also activated.

### Usage
Plugin activation: `-Xfluent-builder`

Options:
#### `-narrow=`y/n
When creating a builder via `copyOf()`, the narrowest possible copy of the input object is instantiated.
See also the documentation on `-Xclone` for a discussion.

#### `-copyPartial=`y/n
If set to true, generates a `copyOf()`-Method that takes an additional `PropertyPath` parameter to limit
the copying to a certain depth or certain nodes in the object tree.


### Limitations

* It generates a large amount of code.

* Note: Shared builder instances are NOT thread-safe by themselves.


## meta

### Motivation
Sometimes, you need information about the properties of a class, or you wish to have a constant for the
names of properties.
The "meta" plugin creates an inner class (the name of which can be controlled by a command-line option),
and adds a constant field for each property.
If the `-extended=y` command´ine option is specified, these constants will hold instances of the `PropertyInfo`
class, on which the name, type, multiplicity (collection or not) and default value (from XSD) are exposed.
Without `-extended`, the constants are simply string constants holding the property names.

### Usage
Plugin activation: `-Xmeta`

Options:
#### `-extended=`y/n (n)
If "y", extended property information (see above) will be generated, if "n", only string constants are generated for each property.
Default: n

#### `-camel-case=`y/n (n)
If "y", generates the constant fields with exactly the same name as the properties they are describing.
If "n" (the default), they will be generated according to the java naming convention for constants.

#### `-meta-class-name=`<string> ("PropInfo")
Specifies the name of the generated nested meta info class. By default, this is "PropInfo", but
because naming conflicts are likely, you can choose a different name here.

#### `-generate-tools=`y/n (y)
As with some of the other plugins, the code generated with the `-extended` option set requires an
additional helper class at compile time. If you say "y" here (the default), this helper class is
generated as part of the generated code, if also the `-extended` option is set to "y".
If you use `-extended` but say "no" here, you will have to add the plugin artifact to the compile (and run)-time
classpath of your generated code.



