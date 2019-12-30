(ns cryogen-asciidoctor-diagram.core
  (:require [cryogen-core.markup :refer [rewrite-hrefs markup-registry]]
            [clojure.string :as s])
  (:import org.asciidoctor.Asciidoctor$Factory
           org.asciidoctor.Options
           org.asciidoctor.SafeMode
           java.util.Collections
           cryogen_core.markup.Markup))

(def ^:private ^:static adoc (Asciidoctor$Factory/create))

(defn asciidoc
  "Returns an Asciidoc (http://asciidoc.org/) implementation of the
  Markup protocol."
  []
  (reify Markup
    (dir [this] "adoc")
    (ext [this] ".adoc")
    (render-fn [this]
      (fn [rdr config]
        (->>
         (.convert adoc
                   (->> (java.io.BufferedReader. rdr)
                        (line-seq)
                        (s/join "\n"))
                   {Options/SAFE (.getLevel SafeMode/SAFE)})
         (rewrite-hrefs (:blog-prefix config)))))))

(defn init []
  (.requireLibrary adoc
   (into-array String
               (list
                "asciidoctor-diagram")))
  (swap! markup-registry conj (asciidoc)))