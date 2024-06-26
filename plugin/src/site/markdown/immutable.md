## immutable
### Motivation
Generally it is advisable to make your business classes immutable as much as possible, to minimise side effects and allow for functional programming patterns.

### Function
This plugin simply makes all "setXXX" methods "protected", thus preventing API consumers to modify state of instances of generated classes after they have been created. This only makes sense together with another plugin that allows for initialization of the instances, like e.g. the included `fluent-builder` plugin. For collection-valued properties, `-Ximmutable` wraps all collections in a `Collections.unmodifiableCollection`, so collections are also made immutable. Because JAXB serialization has a number of constraints regarding the internals of JAXB serializable objects, it wasn't advisable to just remove the setter methods or replace the collections with unmodifiable collections. So, a bit of additional code will be created that leaves the old "mutable" structure of the class intact as much as is needed for JAXB, but modifies the public interface so objects appear immutable to client code.

### Limitations
* Access level "protected" may not be strict enough to prevent state changes.
* If you activate plugins like "fluent-api" or the like, these plugins may circumvent the protection provided by the `immutable` plugin.

### Usage
#### -Ximmutable

#### Options

##### -immutable.fake=`{y|n}` (n)
Do not actually make anything immutable. For test and debug purpose only.


##### -immutable.overrideCollectionClass=`<string>` (null)
Modify collection getters to be declared to return a custom type implementing java.lang.Iterable instead of List.


##### -immutable.constructorAccess=`<string>` (public)
Generate constructors of an immutable class with the specified access level ("public", "private", "protected", "default"). By specification, JAXB needs a public no-arg constructor for marshalling and unmarshalling objects to an from XML. It turns out, however, that many implementations support protected constructors as well.
This option has been included since it doesn't make sense to construct an empty object which then cannot be modified, But anyway, use with caution.

