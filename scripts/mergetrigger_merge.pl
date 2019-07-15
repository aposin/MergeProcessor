#
# Copyright 2019 Association for the promotion of open-source insurance software and for the establishment of open interface standards in the insurance industry (Verein zur Förderung quelloffener Versicherungssoftware und Etablierung offener Schnittstellenstandards in der Versicherungsbranche)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#!/usr/bin/perl
#
# SVN Post Commit Hook which creates truMerge Scripts for the automatic MergeProcessor
#Parameters:
#	REPOS-PATH
#	REV
# Helmut Frohner 2013

#/var/mergetrigger/scripts/mergetrigger_plain.pl
# sudo -u wwwrun perl /var/mergetrigger/scripts/mergetrigger.pl /var/subversion/repositories/ABS_CORE_java 7875


use strict;
use File::Spec;
use File::Basename;
use File::Path qw/mkpath/;


sub main();
sub parseArguments(@);
sub getRepositoryName($);
sub branchToUrl($);
sub getSourceBranch($$);
sub getTargetBranch($);
sub getAuthor($$);
sub getDate($$);
sub getChangedFiles($$);
sub logtime();
sub executeCommand(@);
sub dieWithErrorFile($);

my $folderMergeTrigger = "/var/mergetrigger";
my $folderLogs = "$folderMergeTrigger/logs";
my $folderErrors = "$folderMergeTrigger/errors";
my $folderScripts = "$folderMergeTrigger/scripts";

my $logfileName;
my $repositoryName;
my $urlRepository;
my $repositoryHost = "myserver";


main();

sub main() {
	my $repositoryPath = $ARGV[0];
	my $revision = $ARGV[1];
	print logtime()."main: Started. \$ARGV[0]=[".$ARGV[0]."], \$ARGV[1]=[".$ARGV[1]."].\n";

	my ($repositoryPath, $revision) = parseArguments(@ARGV);

	print logtime()."main: repositoryPath=[$repositoryPath]\n";
	print logtime()."revision=[$revision].\n";


	print logtime()."\n\nTO RERUN FOR THIS REVISION CALL: 'sudo -u wwwrun perl $0 $repositoryPath $revision'\n\n\n";


	print logtime()."main: folderMergeTrigger=[$folderMergeTrigger]\n";
	print logtime()."main: folderLogs=[$folderLogs]\n";
	print logtime()."main: folderErrors=[$folderErrors]\n";
	print logtime()."main: folderScripts=[$folderScripts]\n";

	$repositoryName = getRepositoryName($repositoryPath);
	$urlRepository = "https://$repositoryHost.com/svn/$repositoryName";

	$logfileName = "$repositoryName-r$revision";

	my $revisionStart    = $revision - 1;
	my $revisionEnd      = $revision;

	my $author           = getAuthor($repositoryPath, $revision);
	my $date             = getDate($repositoryPath, $revision);
	my $branchSource     = getSourceBranch($repositoryPath, $revision);
	my $branchTarget     = getTargetBranch($branchSource);
	my $urlBranchSource  = branchToUrl($branchSource);
	my $urlBranchTarget  = branchToUrl($branchTarget);
	my @changedFiles     = getChangedFiles($repositoryPath, $revision);

	my $folderAuthor     = "$folderMergeTrigger/merges/$author";

	my $folderDone       = "$folderAuthor/done";
	my $folderIgnored    = "$folderAuthor/ignored";
	my $folderTodo       = "$folderAuthor/todo";

	{# ensure that all needed folders for the author exist
		if(not -d $folderDone) {
			mkpath($folderDone);
			chmod 0777, $folderDone or dieWithErrorFile(logtime()."Couldn't chmod folderDone=[$folderDone]: $!");
		}
		if(not -d $folderIgnored) {
			mkpath($folderIgnored);
			chmod 0777, $folderIgnored or dieWithErrorFile(logtime()."Couldn't chmod folderIgnored=[$folderIgnored]: $!");
		}
		if(not -d $folderTodo) {
			mkpath($folderTodo);
			chmod 0777, $folderTodo or dieWithErrorFile(logtime()."Couldn't chmod folderTodo=[$folderTodo]: $!");
		}
	}

	my $fileMerge = "$folderTodo/$repositoryName\_r$revision\_$date.merge";

	{ # write the merge file

		my $fileContent;
		{
			my $username = $ENV{LOGNAME} || $ENV{USER} || getpwuid($<);

			$fileContent .= "# Created on host=[$repositoryHost] at datetime=".logtime()."by user[".$username."].\n";
			$fileContent .= "URL_BRANCH_SOURCE=$urlBranchSource\n";
			$fileContent .= "URL_BRANCH_TARGET=$urlBranchTarget\n";
			$fileContent .= "REVISION_START=$revisionStart\n";
			$fileContent .= "REVISION_END=$revisionEnd\n";

			foreach my $changedFile (@changedFiles) {
				$changedFile =~ s/\r?\n$//;
				$fileContent .= "WORKING_COPY_FILE=$changedFile\n";
			}
		}

		{
			open (MERGE_FILE, ">", $fileMerge) or dieWithErrorFile(logtime()."Can't open > $fileMerge: $!\n");
			print MERGE_FILE $fileContent;
			close MERGE_FILE;
		}
	}

	#TODO delete all files in $folderLogs with a date (from file name) older than x days. should actually be done by a cron job...

	print logtime()."main: Finished. fileMerge=[$fileMerge]\n";
}


sub parseArguments(@) {
	my @arguments = @_;
	print logtime()."parseArguments: Started. arguments=[".(scalar @arguments)."]\n";
	my $repositoryPath;
	my $revision;

	if(-d $arguments[0]) {
		$repositoryPath = $arguments[0];
	} else {
		die "TODO Usage repositoryPath does not exist\n";
	}

	if($arguments[1] =~ /^\d+$/) {
		$revision = $arguments[1];
	} else {
		die "TODO Usage revision is not a number.\n";
	}

	if(scalar @arguments != 2) {
		die "TODO Usage wrong number of arguments.\n";
	}

	print logtime()."parseArguments: Finished. repositoryPath=[$repositoryPath], revision=[$revision]\n";
	return ($repositoryPath, $revision);
}


sub getRepositoryName($) {
	my $repositoryPath = $_[0];
	print logtime()."getRepositoryName: Started. repositoryPath=[$repositoryPath]\n";

	my $repositoryName;
	{
		$repositoryName = basename($repositoryPath)
	}

	print logtime()."getRepositoryName: Finished. repositoryName=[$repositoryName]\n";
	return $repositoryName;
}

sub branchToUrl($) {
	my $branch = $_[0];
	print logtime()."branchToUrl: Started. branch=[$branch]\n";

	my $url;
	{
		if($branch eq 'trunk') {
			$url = "$urlRepository/trunk";
		} else {
			$url = "$urlRepository/branches/$branch";
		}
	}

	print logtime()."branchToUrl: Finished. url=[$url]\n";
	return $url;
}

sub getSourceBranch($$) {
	#get source branch. assumes that all changed files in a commit belong to one branch.
	my $repositoryPath = $_[0];
	my $revision = $_[1];
	print logtime()."getSourceBranch: Started. repositoryPath=[$repositoryPath], revision=[$revision]\n";

	my $branchSource;
	{
		my $logStdout = "$folderLogs/$logfileName"."_changed.stdout";
		my $logStderr = "$folderLogs/$logfileName"."_changed.stderr";

		my ($status, $output) = executeCommand("svnlook changed -r $revision $repositoryPath 1>$logStdout 2>$logStderr");
		($status != 0) and dieWithErrorFile(logtime()."ERROR getSourceBranch: Couldn't get changed information. logStdout=[$logStdout], logStderr=[$logStderr], status=[$status], output=[$output]\n");

		open(IN, "<", $logStdout) or dieWithErrorFile(logtime()."Can't open < $logStdout: $!\n");
		my @lines = <IN>;
		close IN;

		#Parse something like "A   trunk/folder/folder/" or "D   foo/trunk/file" or "A   branches/folder/file/" or "D   foo/bar/branches/file" or ...
		foreach my $changed (@lines) {
			$changed =~ s/^.{4}//; #cut off the first characters because we are just interested in the path

			if ($changed =~ /^trunk\//) {
				$branchSource = 'trunk';
				last;
			} elsif($changed =~ /^branches\/([^\/]+)\//) {
				$branchSource = $1;
				last;
			} elsif ($changed =~ /^tags\//) {
				print logtime()."getSourceBranch: No merge needed. Tag creation.\n";
				exit 0;
			}
		}

		dieWithErrorFile(logtime()."Couldn't find a source branch. logStdout=[$logStdout], logStderr=[$logStderr], output=[$output]\n") unless defined $branchSource && $branchSource ne '';
	}

	print logtime()."getRepositoryName: Finished. branchSource=[$branchSource]\n";
	return $branchSource;
}

sub getTargetBranch($) {
	my $branchSource = $_[0];
	print logtime()."getTargetBranch: Started. branchSource=[$branchSource]\n";

	my $branchTarget;
	{
		#read the merge rules
		#this variable comes from the file $fileMergeRules. please edit merge rules only in that file.
		my %branchesTarget;
		my $fileMergeRules = "$folderScripts/mergerules_$repositoryName.pl";
		-f $fileMergeRules or dieWithErrorFile(logtime()."Can't find merge rule file '$fileMergeRules'.\n");

		my $rules;
		{ # read mergerules files
			open IN, "<", $fileMergeRules or dieWithErrorFile(logtime()."Can't open < '$fileMergeRules': $!\n");
			#enable slurp mode
			local $/;
			$rules = <IN>;
			close IN;
		}

		{ # eval mergerules
			eval $rules;
			if ($@) {
				dieWithErrorFile(logtime()."Couldn't eval mergerules file '$fileMergeRules': $@\n");
			}
		}

		{ #check merge rules if the commit in this branch needs to be merged in another branch. Otherwise quit here.
			if(!exists $branchesTarget{$branchSource}) {
				print logtime()."No merge needed for commit. No merge rule for the source branch.\n";
				exit 0;
			}
		}

		$branchTarget = $branchesTarget{$branchSource};
	}


	print logtime()."getRepositoryName: Finished. branchTarget=[$branchTarget]\n";
	return $branchTarget;
}

sub getAuthor($$) {
	my $repositoryPath = $_[0];
	my $revision = $_[1];
	print logtime()."getAuthor: Started. repositoryPath=[$repositoryPath], revision=[$revision]\n";

	my $author;
	{
		my $logStdout = "$folderLogs/$logfileName"."_author.stdout";
		my $logStderr = "$folderLogs/$logfileName"."_author.stderr";

		# my ($status, $output) = executeCommand("/usr/bin/svnlook", "author", "-r", $revision, $repositoryPath);
		my ($status, $output) = executeCommand("svnlook", "author", "-r", $revision, $repositoryPath, "1>$logStdout", "2>$logStderr");
		($status != 0) and dieWithErrorFile(logtime()."ERROR getAuthor: Couldn't get author. logStdout=[$logStdout], logStderr=[$logStderr], status=[$status], output=[$output]\n");

		open(IN, "<", $logStdout) or dieWithErrorFile(logtime()."Can't open < ".$logStdout.": ".$!."\n");
		my @lines = <IN>;
		close IN;

		$author = @lines[0];
		chomp $author;

		#if no author is found put it into lost
		if(!(defined $author) || $author eq '') {
			$author = 'lost';
		}

		$author = lc $author;

		dieWithErrorFile(logtime()."Couldn't find the author of the commit. logStdout=[$logStdout], logStderr=[$logStderr]\n") unless defined $author && $author ne '';
	}

	print logtime()."getAuthor: Finished. author=[$author]\n";
	return $author;
}

sub getDate($$) {
	my $repositoryPath = $_[0];
	my $revision = $_[1];
	print logtime()."getDate: Started. repositoryPath=[$repositoryPath], revision=[$revision]\n";

	#get date of commit
	#change format of date for DATE-FORMATTED 2012-11-02 13:59:16 +0100 (Fri, 02 Nov 2012) -> yyyy-MM-dd_HH-mm-ss
	my $date;
	{
		my $logStdout = "$folderLogs/$logfileName"."_date.stdout";
		my $logStderr = "$folderLogs/$logfileName"."_date.stderr";

		my ($status, $output) = executeCommand("svnlook date -r $revision $repositoryPath 1>$logStdout 2>$logStderr");
		($status != 0) and dieWithErrorFile(logtime()."ERROR getDate: Couldn't get date. logStdout=[$logStdout], logStderr=[$logStderr], status=[$status], output=[$output]\n");

		open(IN, "<", $logStdout) or dieWithErrorFile(logtime()."Can't open < ".$logStdout.": ".$!."\n");
		my @lines = <IN>;
		close IN;

		#cut off redundant part " (Fri, 02 Nov 2012)"
		$date = @lines[0];
		$date =~ s/\s*\(.+\)//g;
		chomp $date;

		$date =~ /(\d+)-(\d+)-(\d+)\s(\d+):(\d+):(\d+)\s([-+])?(\d+)/;
		my ($year, $month, $day, $hour, $minute, $second, $tz)=($1, $2, $3, $4, $5, $6, $7.$8);
		$date = "$year-$month-$day"."_"."$hour-$minute-$second"."_"."$tz";

		dieWithErrorFile(logtime()."ERROR getDate: Couldn't get date of the commit.\n") unless defined $date && $date ne '--_--_';
	}

	print logtime()."getDate: Finished. date=[$date]\n";
	return $date;
}

sub getChangedFiles($$) {
	#get source branch. assumes that all changed files in a commit belong to one branch.
	my $repositoryPath = $_[0];
	my $revision = $_[1];
	print logtime()."getChangedFiles: Started. repositoryPath=[$repositoryPath], revision=[$revision]\n";

	my @changedFiles;
	{
		my $logStdout = "$folderLogs/$logfileName"."_changed.stdout";
		my $logStderr = "$folderLogs/$logfileName"."_changed.stderr";

		my ($status, $output) = executeCommand("svnlook", "changed", "-r", $revision, $repositoryPath, "1>$logStdout", "2>$logStderr");
		($status != 0) and dieWithErrorFile(logtime()."ERROR getChangedFiles: Couldn't get changed information. logStdout=[$logStdout], logStderr=[$logStderr], status=[$status], output=[$output]\n");

		open(IN, "<", $logStdout) or dieWithErrorFile(logtime()."Can't open < $logStdout: $!\n");
		my @lines = <IN>;
		close IN;

		foreach my $changed (@lines) {
			push @changedFiles, $changed;
		}
	}

	@changedFiles = sort @changedFiles;

	print logtime()."getChangedFiles: Finished. changedFiles=[".(scalar @changedFiles)."]\n";
	return @changedFiles;
}


sub logtime() {
	(my $sec, my $min, my $hour, my $mday, my $mon, my $year, my $wday, my $yday, my $isdst) = localtime(time());
	return sprintf("[%04d-%02d-%02d %02d:%02d:%02d] ", ($year+1900), ($mon+1), $mday, $hour, $min, $sec);
}

sub executeCommand(@) {
	#Executes the given parameters as one command and returns return code and output.
	#Example: my ($status, $output) = executeCommand ('/bin/ls', '/');
	#http://www.perlmonks.org/?node_id=454715
	my $command = join(' ',@_);
	# my $command = '\''.join('\' \'',@_).'\'';

	print logtime()."executeCommand: command=[$command].\n";

	my $output = qx{$command};
	my $status = $? >> 8;
	#-------------------------------------------
	# my $status = system($_);
	# my $output = "NOT AVAILABLE";

	return ($status, $output);
}

sub dieWithErrorFile($) {
	#write unsuccessful attemps with the error in an error file in the error folder
	my $error = $_[0];

	my $fileError = $folderErrors."/".$logfileName.".error";
	open(OUT, ">>", $fileError) or die logtime()."Can't open file '".$fileError."' to write error '".$error."'.\n";
	print OUT $error."\n";
	close OUT;

	die $error;
}
