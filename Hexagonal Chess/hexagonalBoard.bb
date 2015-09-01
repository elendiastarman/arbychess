Graphics 800, 600

;AutoMidHandle True

lightCell = LoadImage("cellLight.png")
medCell = LoadImage("cellMed.png")
darkCell = LoadImage("cellDark.png")

MaskImage lightCell, 181, 230, 29
MaskImage medCell, 181, 230, 29
MaskImage darkCell, 181, 230, 29

;hexagon(GraphicsWidth()/2, GraphicsHeight()/2, 50, 0)

rad = 5 ;cells
r = 20 ;cell radius

cx = 200
cy = 220

file = WriteFile("hexagonalChess_cells.txt")

For dv = -rad To rad
	If dv > 0
		start = -rad+dv
	Else
		start = -rad
	EndIf
	
	If dv < 0
		final = rad+dv
	Else
		final = rad
	EndIf
	
	For du = start To final
		
		dx = (du-dv)*(r*3/2)
		dy = (du+dv)*(r*Sqr(3)/2)
		
		If (27+du+dv) Mod 3 = 0
			img = medCell
		ElseIf (27+du+dv) Mod 3 = 1
			img = lightCell
		Else
			img = darkCell
		EndIf
		
		DrawImage img, cx+dx, cy+dy
		
;		num = (dv+rad)*(2*rad)+(du+rad)+1
		
		Color 0, 0, 255
		Text cx+dx, cy+dy, num(du,dv,rad)
;		Text cx+dx, cy+dy, num
		
		s$ = ""
		s = s + num(du,dv,rad) + "|"
		s = s + (cx+dx) + "," + (cy+dy) + "," + (6-(27+du+dv) Mod 3) + "|"
		
		nId = 0
		eId = 0
		sId = 0
		wId = 0
		
		If dv+1 <= rad
			nId = num(du,dv+1,rad)
		EndIf
		If dv-1 >= -rad
			sId = num(du,dv-1,rad)
		EndIf
		
		If du-1 >= start
			wId = num(du-1,dv,rad)
		EndIf
		If du+1 <= final
			eId = num(du+1,dv,rad)
		EndIf
		
		s = s + nId + "," + eId + "," + sId + "," + wId + "||"
		
		;WriteLine(file, s)
		
	Next

Next

;CloseFile(file)

WaitKey
End

Function num(du,dv,rad)
	Return ((dv+rad)*(2*rad) + (du+rad) + 1)
End Function

Function min(a,b)
	If a <= b
		Return a
	Else
		Return b
	EndIf
End Function

Function max(a,b)
	If a >= b
		Return a
	Else
		Return b
	EndIf
End Function