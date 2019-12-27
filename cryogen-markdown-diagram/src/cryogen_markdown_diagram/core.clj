(ns cryogen-markdown-diagram.core
  (:require [cryogen-core.markup :refer [markup-registry rewrite-hrefs]]
            [clojure.string :as s])
  (:import 
    (cryogen_core.markup Markup)
    (org.commonmark.parser Parser)
    (org.commonmark.node AbstractVisitor)
    (org.commonmark.node FencedCodeBlock)
    (org.commonmark.node Image)
    (org.commonmark.renderer.html HtmlRenderer)))

(def ^:private ^:static parser (.build (Parser/builder)))
(def ^:private ^:static renderer (.build (HtmlRenderer/builder)))
(def ^:private ^:static encoder  (net.sourceforge.plantuml.code.TranscoderSmart.))

(defn plantuml->markdown [uml]
  (let [code (.encode encoder uml)]
     (str "https://plantuml.mpcjanssen.nl/svg/" code)))

(def ^:private ^:static  v (proxy [AbstractVisitor] []
  (visit [block]
    (if (and 
         (instance? FencedCodeBlock block)
         (= (.getInfo block) "plantuml"))
       (do
        (.insertAfter block (Image. 
                             (plantuml->markdown (.getLiteral block))
                             "plantuml"))
        (.unlink block))
      (.visitChildren this block)))))

(defn markdown
  "Returns a Markdown (https://daringfireball.net/projects/markdown/)
  implementation of the Markup protocol."
  []
  (reify Markup
    (dir [this] "md")
    (ext [this] ".md")
    (render-fn [this]
      (fn [rdr config]
        (let [s (->> (java.io.BufferedReader. rdr)
                 (line-seq)
                 (s/join "\n"))
              d (.parse parser s)
              _ (.accept d v)]
              (.render renderer d))))))

(defn init []
  (swap! markup-registry conj (markdown)))