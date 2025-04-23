(ns xml-hiccup-test
  (:require [clojure.test :refer :all]
            [dk.cst.xml-hiccup :refer :all]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(deftest input-vs-output
  (is (= (parse (io/file "test/test-1307-anno-tei.xml"))
         (parse (io/input-stream "test/test-1307-anno-tei.xml"))
         (parse (slurp "test/test-1307-anno-tei.xml"))
         (edn/read-string (slurp "test/test-1307-anno-tei.edn")))))

(deftest file-metadata
  (is (nil? (meta (parse (clojure.java.io/file "test/test-1307-anno-tei.xml")))))
  (is (= (meta (parse (clojure.java.io/file "test/test-1307-anno-tei.xml")
                      {:file-meta {:path     :absolute
                                   :filename true}}))
         {:filename "test-1307-anno-tei.xml",
          :path     "/Users/rqf595/Code/xml-hiccup/test/test-1307-anno-tei.xml"}))
  (is (= (meta (parse (clojure.java.io/file "test/test-1307-anno-tei.xml")
                      {:file-meta {:path true}}))
         {:path "test/test-1307-anno-tei.xml"})))
