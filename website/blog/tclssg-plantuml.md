{
    title {Adding code highlight to TclSSG}
    author {Mark Janssen}
    presets blog
    tags {tclssg}
    date 2020-06-23
}

Out of the box, TclSSG doesn't support code highlighting. Adding it is trivial though.


<!-- more -->

With the [commit](https://github.com/tclssg/tclssg/commit/ed131c09e2944bfd78766fdffed714afd43e06bb) adding code blocks with language classes to the markdown parser, adding code highlighting using highlightjs is trivial.

- Download highlighjs with the required languages from https://highlightjs.org/download/.
- Extract the highlightjs bundle in the `static/vendor` directory of the website.
- Add the required css and js includes to the html headers.

For each applicable preset in `presets` at the following stanza:

``` tcl
{head top} {
<link rel="stylesheet" href="/vendor/highlightjs/styles/github.css">
<script src="/vendor/highlightjs/highlight.pack.js"></script>
<script>hljs.initHighlightingOnLoad();</script>
}
```

You can replace `github` with any supported style.

