DaVinch
==========

QA system forked from OpenEphyra.

#Preparation

1. Exec ```./gradlew``` on root directory
2. create config.inc based on config_sample.inc
3. install ant if you have not installed yet
4. Exec ```./compile-openephyra.sh``` on root directory
5. install thrift based on [this instruction](http://www.saltycrane.com/blog/2011/06/install-thrift-ubuntu-1010-maverick//)  

#Run

Exec ```./gradlew run```

If you want to add args, do like this(but default not necessary):  

    ./gradlew run -Pargs="hoge fuga piyo"
    

    

    

    

    
