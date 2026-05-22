(ns dk.cst.xml-hiccup
  "Clojure(Script) implementation of an XML parser that, more or less directly,
  converts an XML file into Hiccup data without trying to be clever about it.

  Comments and superfluous whitespace are deliberately not preserved, while
  namespaces are converted into regular Clojure namespaces. No attempt is made
  to map namespace URIs to namespace aliases.

  See: https://developer.mozilla.org/en-US/docs/Web/API/Node/nodeType"
  (:require [clojure.string :as str])
  #?(:clj (:import [org.w3c.dom Document Element Text Comment Node NamedNodeMap]
                   [javax.xml.parsers DocumentBuilderFactory DocumentBuilder]
                   [java.io ByteArrayInputStream InputStream File])))

#?(:cljs (do
           (def Document js/Document)
           (def Element js/Element)
           (def Text js/Text)
           (def Comment js/Comment)
           (def Node js/Node)))

(defn parser []
  #?(:clj  (.newDocumentBuilder (DocumentBuilderFactory/newInstance))
     :cljs (js/DOMParser.)))

(defn keywordize
  "Keywordize `s` converting XML namespaces to Clojure namespaces."
  [s]
  (let [[s1 s2] (str/split s #":")]
    (if s2
      (keyword s1 s2)
      (keyword s1))))

(defn dom-parse
  "Parse `xml` into a DOM tree. Gets a new parser whenever this is called
   because the parsers are not thread safe."
  [xml]
  (let [parser (parser)]
    #?(:clj  (cond
               (string? xml)
               (dom-parse (ByteArrayInputStream. (.getBytes xml)))

               (instance? InputStream xml)
               (.parse ^DocumentBuilder parser ^InputStream xml)

               (instance? File xml)
               (.parse ^DocumentBuilder parser ^File xml))

       :cljs (.-firstChild (.parseFromString parser xml "text/xml")))))

(defn attribute-objects
  "Retrieve the raw attribute objects from the `node`."
  [^Node node]
  #?(:clj  (let [named-node-map (.getAttributes node)]
             (for [n (range (.getLength named-node-map))]
               (.item named-node-map n)))
     :cljs (.-attributes node)))

(defn attribute-key
  "Retrieve the key from the `attribute` object."
  [^Node attribute]
  #?(:clj  (.getNodeName attribute)
     :cljs (.-name attribute)))

(defn attribute-val
  "Retrieve the value from the `attribute` object."
  [^Node attribute]
  #?(:clj  (.getNodeValue attribute)
     :cljs (.-value attribute)))

(defn node-attrs
  "Get a Hiccup attributes map from a `node`."
  [^Node node]
  (into {} (for [attribute (attribute-objects node)]
             [(keywordize (attribute-key attribute))
              (attribute-val attribute)])))

(defn node-tag
  "Get a Hiccup tag from a `node`."
  [^Node node]
  (keywordize #?(:clj  (.getNodeName node)
                 :cljs (.-tagName node))))

(defn node-children
  "Get the children of the `node` as objects."
  [^Node node]
  #?(:clj  (let [node-list (.getChildNodes node)]
             (for [n (range (.getLength node-list))]
               (.item node-list n)))
     :cljs (.-childNodes node)))

(defn whole-text
  [^Text node]
  #?(:clj  (.getWholeText node)
     :cljs (.-wholeText node)))

(defn node->hiccup
  "Recursively convert a `node` and its children to Hiccup."
  [node]
  (condp instance? node

    Document
    #?(:clj  (node->hiccup
               (doto (.getDocumentElement ^Document node)
                 (.normalize)))

       ;; TODO: is the CLJS part relevant?
       :cljs (map node->hiccup (node-children node)))

    Element
    (into [(node-tag node) (node-attrs node)]
          (comp
            (map node->hiccup)
            (remove nil?))
          (node-children node))

    ;; TODO: should probably escape HTML here
    Text
    (let [s (whole-text node)]
      (when (not (str/blank? s))
        s))

    Comment
    nil

    ;; default case
    node))

(defn parse
  "Convert `xml` (a String, File, or InputStream) into a tree of Hiccup data.

  If the `xml` is a File object, you may provide a :file-meta map in the `opts`
  to specify relevant metadata for the output, setting :filename or :path to a
  truthy value.

  If you want the absolute file path as metadata, you can state this as:
     (parse xml {:file-meta {:path :absolute}})."
  ([xml]
   (parse xml nil))
  ([xml {:keys [file-meta]}]
   (let [hiccup (node->hiccup (dom-parse xml))]
     #?(:clj  (if (and (instance? File xml) (not-empty file-meta))
                (let [{:keys [filename path abspath]} file-meta]
                  (with-meta
                    hiccup
                    (cond-> {}
                      filename (assoc :filename (.getName ^File xml))
                      path (#(if (= path :absolute)
                               (assoc % :path (.getAbsolutePath ^File xml))
                               (assoc % :path (.getPath ^File xml)))))))
                hiccup)
        :cljs hiccup))))

(comment
  ;; Create Hiccup for testing
  (->> (parse (clojure.java.io/file "test/test-1307-anno-tei.xml"))
       (clojure.pprint/pprint)
       (with-out-str)
       (spit "test/test-1307-anno-tei.edn"))
  #_.)
