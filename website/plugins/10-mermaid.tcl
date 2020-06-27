# Copyright (c) 2020, M.P.C. Janssen
# This code is released under
# the terms of the MIT license. See the file LICENSE for details.


namespace eval ::tclssg::pipeline::10-mermaid {
    namespace path ::tclssg


    proc mermaid {raw} {
        set f [file tempfile mmdname mmd]
        puts -nonewline $f $raw
        close $f
        exec mmdc -i $mmdname
        set f [open $mmdname.svg r]
        set out [read $f]
        close $f
        file delete $mmdname
        file delete $mmdname.svg
        return <div>$out</div>
    }
 
    proc transform {} {
        set me [namespace tail [namespace current]]
        log::info [list running demo plugin $me]
        log::debug $::tclssg::interpreter::aliases

        dict set ::tclssg::interpreter::aliases \
            [namespace current]::mermaid\
            mermaid
    }
}
