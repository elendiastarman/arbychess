Function hexagon(cx, cy, radius, filled=0)

	If filled = 0
		
		For k = 0 To 5
			Line cx+radius*Cos(60*k), cy+radius*Sin(60*k), cx+radius*Cos(60*(k+1)), cy+radius*Sin(60*(k+1))
		Next
		
	Else
		
		Line cx-radius, cy, cx+radius, cy
		
		ylim = radius*Sqr(3)/2.
		For dy = 1 To ylim
			dx# = radius - dy*Sqr(3)/3.
			Line cx-dx, cy+dy, cx+dx, cy+dy
			Line cx-dx, cy-dy, cx+dx, cy-dy
		Next
		
	EndIf

End Function

;hexagon(GraphicsWidth()/2, GraphicsHeight()/2, 50, 0)

ClsColor 181, 230, 29

lightCell = CreateImage(40,40)
SetBuffer(ImageBuffer(lightCell))
Cls
Color 255, 206, 158
hexagon(ImageWidth(lightCell)/2, ImageHeight(lightCell)/2, 20, 1)
SaveImage(lightCell, "cellLight.png")

medCell = CreateImage(40,40)
SetBuffer(ImageBuffer(medCell))
Cls
Color 232, 171, 111
hexagon(ImageWidth(medCell)/2, ImageHeight(medCell)/2, 20, 1)
SaveImage(medCell, "cellMed.png")

darkCell = CreateImage(40,40)
SetBuffer(ImageBuffer(darkCell))
Cls
Color 209, 139, 71
hexagon(ImageWidth(darkCell)/2, ImageHeight(darkCell)/2, 20, 1)
SaveImage(darkCell, "cellDark.png")

allowCell = CreateImage(40,40)
SetBuffer(ImageBuffer(allowCell))
Cls
Color 0, 255, 0
hexagon(ImageWidth(allowCell)/2, ImageHeight(allowCell)/2, 20, 1)
Color 181, 230, 29
hexagon(ImageWidth(allowCell)/2, ImageHeight(allowCell)/2, 17, 1)
SaveImage(allowCell, "cellAllowed.png")

selectCell = CreateImage(40,40)
SetBuffer(ImageBuffer(selectCell))
Cls
Color 164, 72, 164
hexagon(ImageWidth(selectCell)/2, ImageHeight(selectCell)/2, 20, 1)
Color 181, 230, 29
hexagon(ImageWidth(selectCell)/2, ImageHeight(selectCell)/2, 17, 1)
SaveImage(selectCell, "cellSelected.png")

denyCell = CreateImage(40,40)
SetBuffer(ImageBuffer(denyCell))
Cls
Color 255, 0, 0
hexagon(ImageWidth(denyCell)/2, ImageHeight(denyCell)/2, 20, 1)
Color 181, 230, 29
hexagon(ImageWidth(denyCell)/2, ImageHeight(denyCell)/2, 17, 1)
SaveImage(denyCell, "cellDenied.png")

End