#!/bin/sh

# Some basic info about the environment
echo "Running on:"
uname -a

echo "G++ version"
g++ --version

echo "JVM version"
java -version
which java

echo "Java compiler version"
javac -version
which javac

# Set the HOME environment
echo "The current directory is $PWD"
echo "Home directory is $HOME by default"
export HOME=$PWD
echo "After the change home directory is $HOME"

export REPOSITORY=$HOME/repo/

CODE_DIR="joblog"
echo "Checking if Git is installed on the node..."
which git
if [ $? -eq 0 ]; then
    # Get the analysis code from Github
    echo "Git found! Getting the analysis code from github"
    git clone http://github.com/kaitanie/joblog.git
    echo "Using code version $(git describe --tags)"
else
    echo "Git not installed on this machine. Downloading a boring old tarball."
    # Untar the code
    curl -fksSL https://github.com/kaitanie/joblog/tarball/master > joblog.tar.gz
    tar -zxf joblog.tar.gz
    CODE_DIR=$(ls -1 | grep kaitanie-joblog)
    echo "Using code directory $CODE_DIR"
fi

echo "Switching to $CODE_DIR"

cd $CODE_DIR
./lein-script deps
./lein-script protobuf

echo "Checking if leiningen can be installed and runs..."
./lein-script version

# Check that the code works
echo "Running the unit tests"
./lein-script midje

# And run it
./lein-script run convert ../accounting-korundi-since-2008-01-01.log ../accounting-korundi-since-2008-01-01.pb

