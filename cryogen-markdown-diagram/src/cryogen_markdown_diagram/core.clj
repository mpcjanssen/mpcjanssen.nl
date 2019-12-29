(ns cryogen-markdown-diagram.core
  (:require [cryogen-core.markup :refer [markup-registry rewrite-hrefs]]
            [clojure.string :as s])
  (:import 
   (cryogen_core.markup Markup)
   (org.commonmark.ext.heading.anchor HeadingAnchorExtension)
   (org.commonmark.parser Parser)
   (org.commonmark.node AbstractVisitor)
   (org.commonmark.node FencedCodeBlock)
   (org.commonmark.node Image)
   (org.commonmark.renderer.html HtmlRenderer)))

(def ^:private ^:static parser (.build (Parser/builder)))
(def ^:private ^:static renderer 
 (.build 
  (.extensions  
   (HtmlRenderer/builder) 
   (java.util.ArrayList. (list (HeadingAnchorExtension/create))))))

(def ^:private ^:static encoder  (net.sourceforge.plantuml.code.TranscoderSmart.))

(defn plantuml->url [server uml]
  (let [code (.encode encoder uml)]
     (str server "/svg/" code)))

(defn plantuml-visitor [server-url] 
  (proxy [AbstractVisitor] []
    (visit [block]
      (if (and 
            (instance? FencedCodeBlock block)
            (= (.getInfo block) "plantuml"))
          (do
           (.insertAfter block (Image. 
                                (plantuml->url server-url (.getLiteral block))
                                "plantuml"))
           (.unlink block))
          (.visitChildren this block)))))

(defn markdown
  []
  (reify Markup
    (dir [this] "md")
    (ext [this] ".md")
    (render-fn [this]
      (fn [rdr config]
        (let [s (->> (java.io.BufferedReader. rdr)
                     (line-seq)
                     (s/join "\n"))
              doc (.parse parser s)
              _ (.accept doc (plantuml-visitor (:plantuml-url config)))]
            (rewrite-hrefs 
              (:blog-prefix config)
              (.render renderer doc)))))))

(defn init []
  (swap! markup-registry conj (markdown)))