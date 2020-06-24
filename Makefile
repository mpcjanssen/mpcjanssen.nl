.PHONY: deploy build open


deploy:
	c:/Users/Mark/Src/site-tcl/scripts/tclssg/ssg.cmd build --plugins
	c:/Users/Mark/Src/site-tcl/scripts/tclssg/ssg.cmd deploy-custom

build:
	c:/Users/Mark/Src/site-tcl/scripts/tclssg/ssg.cmd build --plugins --local


open:
	cmd /c start http://localhost

