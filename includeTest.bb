Graphics 800, 600

;filename$ = Input$("type file name")
filename$ = "incT1.bb"
;Include Chr$(34)+Str(filename)+Chr$(34)
;Include ""+filename
;ExecFile(filename)

Function foo()
	;Include filename
	Include "incT1.bb"
End Function



;out()

;WaitKey
End