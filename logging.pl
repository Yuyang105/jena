#!/bin/perl
# find src -name .svn -prune -o -name \*java -print | xargs -n 1 perl -i.bak SCRIPT

undef $/ ;
$_ = <> ;
s/import org.apache.commons.logging.Log\s*;/import org.slf4j.Logger;/ ;
s/import org.apache.commons.logging.LogFactory\s*;/import org.slf4j.LoggerFactory;/ ;

s/ Log / Logger / ;
s/LogFactory.getLog/LoggerFactory.getLogger/ ;
s/.fatal/.error/ ;

print $_ ;
