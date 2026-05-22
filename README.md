XML Hiccup
==========
Fork of [kuhumcst/xml-hiccup](https://github.com/kuhumcst/xml-hiccup) that's been modified to be thread-safe.

Clojure implementation of an XML parser that, more or less directly,
converts an XML file into Hiccup data without trying to be clever about it.

Comments and superfluous whitespace are deliberately not preserved, while
namespaces are converted into regular Clojure namespaces. No attempt is made
to map namespace URIs to namespace aliases.

Usage
-----
This example shows how to parse a `File`, though it could also be a `String` or an `InputStream`. 

```clojure
(require '[dk.cst.xml-hiccup :as xh]
         '[clojure.java.io :as io])

(xh/parse (io/file "test/test-1307-anno-tei.xml"))
```
