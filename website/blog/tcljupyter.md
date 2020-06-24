{
    title {Tcl Jupyter Kernel}
    author {Mark Janssen}
    presets blog
    tags {jupyter tcl}
    date 2020-06-23
}


Embedding plantuml in tclssg 

<!-- more -->

``` tcl
set a 4
puts ok
```


``` plantuml
A -> B: hello world
```

<%! plantuml [db input get data/design.puml raw] %>
