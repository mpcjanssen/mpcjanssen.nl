.PHONY: cli server sub

cli: sub
	lein run

sub:
	lein sub install
   
server: sub
	lein ring server