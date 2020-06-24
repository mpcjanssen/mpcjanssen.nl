# Copyright (c) 2020, M.P.C. Janssen
# This code is released under
# the terms of the MIT license. See the file LICENSE for details.

package require tls
package require http
package require base64
::http::register https 443 [list ::tls::socket -autoservername 1]

namespace eval ::tclssg::pipeline::10-register-template-command {
    namespace path ::tclssg


    proc plantumlurl {raw} {
        return https://plantuml.mpcjanssen.nl/svg/[string map [list \n {}] [string trim [base64toplantuml [base64::encode [zlib deflate $raw]]]]]
    }
 
    variable base64map
    foreach x {A B C D E F G H I J K L M N O P Q R S T U V W X Y Z  a b c d e f g h i j k l m n o p q r s t u v w x y z 0 1 2 3 4 5 6 7 8 9 + /}\
        y {0 1 2 3 4 5 6 7 8 9 A B C D E F G H I J K L M N O P Q R S T U V W X Y Z a b c d e f g h i j k l m n o p q r s t u v w x y z - _} {
            lappend base64map $x $y
    }
    interp alias {} [namespace current]::base64toplantuml {} string map $base64map 

    proc plantuml {uml} {
        set tok [http::geturl [plantumlurl $uml]]
        set data [http::data $tok]
        http::cleanup $tok
        return "<div>$data</div>"
    }

    proc transform {} {
        set me [namespace tail [namespace current]]
        log::info [list running demo plugin $me]
        log::debug $::tclssg::interpreter::aliases

        dict set ::tclssg::interpreter::aliases \
            [namespace current]::plantuml \
            plantuml
    }
}
