  
(defproject cryogen-markdown-diagram "0.0.1"
    :description "Markdown parser for Cryogen with diagram support"
    :url "https://github.com/mpcjanssen/cryogen-markdown-diagram"
    :license {:name "Eclipse Public License"
              :url "http://www.eclipse.org/legal/epl-v10.html"}
    :dependencies [[org.clojure/clojure "1.7.0"]
                   [cryogen-core "0.1.25"]
                   [com.atlassian.commonmark/commonmark-ext-heading-anchor "0.13.1"]
                   [net.sourceforge.plantuml/plantuml "8059"]
                   [com.atlassian.commonmark/commonmark "0.13.1"]])