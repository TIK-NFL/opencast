#!/bin/bash

FUNCTIONS="functions.sh"
. ${FUNCTIONS}

#The comments in this file assume you are creating a tag from a release branch
#This script does *not* push any changes, so it should be safe to experiment with locally

#The version the POMs are in the release branch.
#E.g. BRANCH_VER=1.3-SNAPSHOT
BRANCH_VER=1.6.0-SNAPSHOT

#The new version of our release as it will show up in the tags
#E.g. RELEASE_VER=1.3-rc5
RELEASE_VER=1.3.0-beta1

#The jira ticket this work is being done under (must be open)
JIRA_TICKET=MH-10473

#=======You should not need to modify anything below this line=================

WORK_DIR=../../../

yesno -d no "We need a GPG key available.  If you have one setup please continue, otherwise create one.  Do you have a GPG key ready to go?" has_key
if [[ ! "$has_key" ]]; then 
  exit 1
fi
keyOpts=""

while [[ true ]]; do
  ask -d "Use my default GPG key" "Do you want to use a key other than your default key?  If so, please enter the key ID now, otherwise just hit enter to use your default GPG key." new_key_id
  if [[ "$new_key_id" != "Use my default GPG key" ]]; then
    if [[ ! "`gpg --list-secret-keys | grep $new_key_id`" ]]; then
      "No key with that ID found..."
    else
      #Fake autocomplete, but it works
      new_key_id=`gpg --list-secret-keys | grep $new_key_id | sed 's/.*\/\('$new_key_id'.*\) .*/\1/g'`
      keyOpts="-u $new_key_id"
      break
    fi
  else
    break
  fi
done  

#Reset this script so that the modifications do not get committed
git checkout -- tag.sh

choose -t "Are you cutting an RC, or a final release of $curBranch?" "RC" "Final Release" RELEASE_TYPE

echo "Replacing POM file version in the POMs."
updatePomVersions -w $WORK_DIR -o $BRANCH_VER -n $RELEASE_VER

case "$RELEASE_TYPE" in
0)
    # Release candidate
    git commit -a -m "$JIRA_TICKET: Committing $RELEASE_VER to branch to contain POM changes and tag"
    git tag $keyOpts -s $RELEASE_VER -m "Release $RELEASE_VER"
    git revert --no-edit HEAD

    echo "Summary:"
    echo "-Modified pom files, and tagged $RELEASE_VER"
    echo "We can push these changes to the public repo if you want."
    yesno -d no "Do you want this script to do that automatically for you?" push
    if [[ "$push" ]]; then
        git push origin
        git push --tags origin
    fi
    ;;
1)
    #Final release
    git commit -a -m "$JIRA_TICKET: Committing $RELEASE_VER directly to $curBranch in preparation for final release."

    #Final version of the branch into master, and tag!
    git checkout master
    git merge --no-ff r/$RELEASE_VER
    git tag $keyOpts -s $RELEASE_VER -m "Release $RELEASE_VER"

    #Pop this off the release branch since we do not want the POM version changes in develop
    #(remember that we changed them with the branch script)
    git checkout r/$RELEASE_VER
    git revert --no-edit HEAD

    #Merge everything into develop
    git checkout develop
    git merge --no-ff r/$RELEASE_VER
    git branch -d r/$RELEASE_VER

    echo "Summary:"
    echo "-Merged r/$RELEASE_VER into master"
    echo "-Merged r/$RELEASE_VER into develop"
    echo "-Deleted local r/$RELEASE_VER"
    echo "We can push these changes to the public repo, and delete the remote branch for you as well."
    yesno -d no "Do you want this script to do that automatically for you?" push
    if [[ "$push" ]]; then
        git push origin develop
        git push origin master
        git push --tags origin
        git push origin :r/$RELEASE_VER
    fi
    ;;
esac
