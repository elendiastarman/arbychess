file = WriteFile("chessBoardCells.txt")

For y = 1 To 8
	For x = 1 To 8
		s$ = ""
		
		id = 8*(y-1)+x
		
		s = s + Str(id)+"|"
		
		imgx = 30+20*x
		imgy = 30+20*y
		If (x+y) Mod 2 = 0
			imgsrc$ = Chr(34)+"cellWhite.png"+Chr(34)
		Else
			imgsrc$ = Chr(34)+"cellBlack.png"+Chr(34)
		EndIf
		
		s = s + " "+Str(imgx)+", "+Str(imgy)+","+imgsrc+"|"
		
		If y > 1
			northId = 8*(y-2)+x
		Else
			northId = 0
		EndIf
		If x < 8
			eastId = 8*(y-1)+x+1
		Else
			eastId = 0
		EndIf
		If y < 8
			southId = 8*(y)+x
		Else
			southId = 0
		EndIf
		If x > 1
			westId = 8*(y-1)+x-1
		Else
			westId = 0
		EndIf
		
		s = s + Str(northId)+","+Str(eastId)+","+Str(southId)+","+Str(westId)+"|"
		
		pieceId = -1
		
		If y = 2 ;white pawns
			pieceId = 1
		ElseIf y = 7 ;black pawns
			pieceId = 2
		ElseIf y = 1 ;white pieces
			If x = 1 Or x = 8 ;rooks
				pieceId = 7
			ElseIf x = 2 Or x = 7 ;knights
				pieceId = 5
			ElseIf x = 3 Or x = 6 ;bishops
				pieceId = 3
			ElseIf x = 4 ;king
				pieceId = 11
			ElseIf x = 5 ;queen
				pieceId = 9
			EndIf
		ElseIf y = 8 ;black pieces
			If x = 1 Or x = 8 ;rooks
				pieceId = 8
			ElseIf x = 2 Or x = 7 ;knights
				pieceId = 6
			ElseIf x = 3 Or x = 6 ;bishops
				pieceId = 4
			ElseIf x = 4 ;king
				pieceId = 12
			ElseIf x = 5 ;queen
				pieceId = 10
			EndIf
		EndIf
		
		If pieceId = -1
			s = s + "|"
		Else
			s = s + Str(pieceId)+",10,10|"
		EndIf
		
		If y = 1 Or y = 2 ;white player
			s = s + "1"
		ElseIf y = 7 Or y = 8 ;black player
			s = s + "2"
		EndIf
		
		WriteLine(file,s)
		
	Next
Next

CloseFile(file)
End