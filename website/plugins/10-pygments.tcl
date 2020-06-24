# Copyright (c) 2020, M.P.C. Janssen
# This code is released under
# the terms of the MIT license. See the file LICENSE for details.


namespace eval ::tclssg::pipeline::10-pygments {
    namespace path ::tclssg


    proc pygmentize {language raw} {
        return [exec pygmentize -f html -l $language << $raw]
    }
 
    proc transform {} {
        set me [namespace tail [namespace current]]
        log::info [list running demo plugin $me]
        log::debug $::tclssg::interpreter::aliases

        dict set ::tclssg::interpreter::aliases \
            [namespace current]::pygmentize \
            pygmentize
    }
}
