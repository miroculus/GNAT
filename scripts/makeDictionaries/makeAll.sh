echo "Making list, dict, mwt, automaton for species $1";
./makeListFromGeneInfo.sh $1
./makeDictFromList.sh $1
./makeMwt.sh $1
./makeAutomaton.sh $1
echo "Done."
