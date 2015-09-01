Include "splitting.bb"

test$ = "the quick brown fox jumps over the lazy dog"
testL.splitList = split(test)

L = splitListLength(testL)
Print L

;While testL <> Null
;	Print testL\s
;	testL = testL\nex
;Wend

Print "-"

For i = 0 To L+1
	Print accessElement(testL, i)
Next

Print "-"

Local dats$[1000]
splitListToArray(testL, dats)

For j = 0 To Int(dats[0])
	Print dats[j]
Next

Print "-"

deleteSplitList(testL)

For spl.splitList = Each splitList
	Print spl\s
Next

WaitKey
End