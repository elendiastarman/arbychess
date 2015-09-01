Type splitList
	Field s$
	Field bef.splitList, nex.splitList
End Type

Function split.splitList(source$, delim$="", block=0) ;delim = "" -> split on whitespace, block=1 -> delim is treated as one unit

	sL.splitList = Null

	If delim = ""
		delim = " "+Chr(9)+Chr(10)+Chr(13)
	EndIf

	If block = 0
		chunk = 1
	Else
		chunk = Len(delim)
	EndIf

	oldpos = 0
	pos = 1
	While pos <= Len(source)
		
		part$ = Mid$(source, pos, chunk)
		If Instr(delim, part, 1) > 0
		
			slice$ = Mid$(source, oldpos+1, pos-oldpos-1)
		
			If sL = Null
				sL = New splitList
			Else
				sL\nex = New splitList
				sL\nex\bef = sL
				sL = sL\nex
			EndIf
			
			sL\s = slice
			pos = pos + chunk-1
			oldpos = pos
		
		EndIf
		
		pos = pos + 1
	Wend
	
	part$ = Mid$(source, pos, chunk)
	If Instr(delim, part, 1) > 0
	
		slice$ = Mid$(source, oldpos+1, pos-oldpos-1)
	
		If sL = Null
			sL = New splitList
		Else
			sL\nex = New splitList
			sL\nex\bef = sL
			sL = sL\nex
		EndIf
		
		sL\s = slice
		oldpos = pos
	
	EndIf
	
	While sL\bef <> Null
		sL = sL\bef
	Wend
	
	Return sL

End Function

Function splitListToArray(sL.splitList, array$[1000])

	If sL = Null
		Return
	EndIf
	
	array[0] = 0
	While sL <> Null
		array[0] = Str(Int(array[0])+1)
		array[ Int(array[0]) ] = sL\s
		If sL\nex <> Null
			sL = sL\nex
			Delete sL\bef
		Else
			Delete sL
		EndIf
	Wend

End Function

Function accessElement$(sL.splitList, index)

	If sL = Null
		Return ""
	EndIf

	If index <= 0
		Return ""
	EndIf
	
	elem$ = ""
	
	i = 0
	While sL <> Null
		i = i + 1
		If i = index
			elem = sL\s
			Exit
		EndIf

		If sL\nex = Null
			Exit
		Else
			sL = sL\nex
		EndIf
	Wend
	
	While sL\bef <> Null
		sL = sL\bef
	Wend
	
	Return elem

End Function

Function splitListLength(sL.splitList)

	If sL = Null
		Return 0
	EndIf

	l = 0
	While sL\nex <> Null
		l = l + 1
		sL = sL\nex
	Wend
	l = l + 1
	
	While sL\bef <> Null
		sL = sL\bef
	Wend
	
	Return l

End Function

Function deleteSplitList(sL.splitList)

	If sL = Null
		Return
	EndIf

	While sL\nex <> Null
		sL = sL\nex
		Delete sL\bef
	Wend
	Delete sL

End Function