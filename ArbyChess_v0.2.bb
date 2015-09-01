;;;
;
; Author: Lee Burnette/El'endia Starman
; Date: 2013-03-30
;
; Name: ArbyChess
; Function: A chess engine that allows arbitrary boards and piece moves
;
;;;

; ; graphics selector goes here ; ;
.Graphics

Graphics 800, 600
initialize()
WaitKey
End

; types ;
.types

Type board
	Field cells.celloplayer ;Ha! Pun!
	Field ramifying ;oversimply, whether a bishop can go the same distance in the same direction and end up in one of two places
End Type

Type cell
	Field x, y ;for drawing image
	Field px, py ;for positioning piece image
	Field img
	Field dullhorse.cell[4] ;Ha! Another pun!
	Field dullhorseIds[4]
	Field p.piece
	Field id
End Type

Type piece
	Field x, y ;for drawing image
	Field img
	Field c.cell ;which cell the piece is on
	Field m.movelist ;available moves
	Field mobile
	Field alive
	Field name$
	Field id
	Field players.player[10] ;allow the possibility of multiple players controlling the same piece
	;Field ...
End Type

Type move
	Field dx, dy
	Field vector ;values of 1-9 ; "momentum" direction
	Field options[10] ;flags
	Field counter[10] ;counting number of moves, etc.
	Field nex.movelist ;available next moves
	Field nexIds[1000];
	Field id
End Type

Type movelist
	Field m.move
	Field bef.movelist, nex.movelist ;doubly-linked list
End Type

Type celloplayer
	Field c.cell
	Field bef.celloplayer, nex.celloplayer ;doubly-linked list
End Type

Type mouse
	Field x, y
	Field c.cell
	Field selector ;if a cell has been selected
End Type

Type game
	Field b.board
	Field m.mouse
	;Field c.cards
End Type

Type player
	Field id
	Field counter[10] ;score, time, etc.
	Field name$
End Type

; functions ;
.functions

;game functions
.gameFuncs
Function initialize.game()

	g.game = New game
	b.board = New board
	m.mouse = New mouse
	
	g\b = b
	g\m = m
	
	;Stop
	
	success = import(b, "hex.txt") ;for now, the simple game called Hex
	
	If success
		run(g)
	Else
		DebugLog "Import failed."
		finish(g.game)
	EndIf

End Function

Function run(g.game)

	ClsColor 100, 100, 100
	Cls

	display(g\b)
	
	WaitKey

	finish(g)

End Function

Function finish(g.game)

	DebugLog "Exiting..."

	For pc.piece = Each piece
		Delete pc
	Next
	DebugLog "Pieces deleted."
	
	For ce.cell = Each cell
		Delete ce
	Next
	DebugLog "Cells deleted."
	
	For mo.move = Each move
		Delete mo
	Next
	DebugLog "Moves deleted."
	
	For ml.movelist = Each movelist
		Delete ml
	Next
	DebugLog "Movelist cleared."
	
	For cl.celloplayer = Each celloplayer
		Delete cl
	Next
	DebugLog "Celloplayer cleared."
	
	For mu.mouse = Each mouse
		Delete mu
	Next
	DebugLog "Mouse deleted."
	
	For bo.board = Each board
		Delete bo
	Next
	DebugLog "Board deleted."
	
	For py.player = Each player
		Delete py
	Next
	DebugLog "Players deleted."
	
	For ga.game = Each game
		Delete ga
	Next
	DebugLog "Game ended."
	
	End

End Function

;board functions
.boardFuncs
Function import(b.board, path$="")

	;;
	;assumed file format:
	;  "players"
	;  id | name$ | [counter values]
	;  ""
	;  "moves"
	;  id,dx,dy | move,capture | [counter values] | next moves' ids
	;  ""
	;  "pieces"
	;  id | imgx,imgy,imgsrc | mobile,name$ | moves' ids
	;  ""
	;  "cells"
	;  id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id] | [player(s) id(s)]
	;;
	

	If path = "" ;file chooser
		;put navigation here
	EndIf

	file = ReadFile(path)
	
	segment$ = ReadLine$(file) ;another pun!
	While segment = "" Or Mid$(segment, 1, 1)=";" ;blank line or comment (starts with ;)
		segment = ReadLine(file)
	Wend
	
	;WaitKey
	
	;players
	;segment = ReadLine$(file)
	If segment = "players"
		DebugLog "players"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id | name$ | [counter values]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				pl.player = New player
				
				pl\id = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				pl\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
					
				pos = Instr(segment, "|", pos+1)
				
				If pos+1 < Len(segment) ;if there is a counter value

					If Instr(segment, ",", pos+1) = 0 ;if there's only one value
						pl\counter[1] = Int( Mid$( segment, pos+1, Len(segment)-pos-1 ) )
						pl\counter[0] = 1
					Else ;comma separated list
						segment = segment+",," ;bit of a trick
						While Instr(segment, ",", pos+1) < Len(segment)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							pl\counter[ pl\counter[0] ] = tempC
							pl\counter[0] = pl\counter[0]+1
							
							If pl\counter[0] > 9
								Exit
							EndIf
						Wend
					EndIf
				
				EndIf
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
	
	Else
		Return 0
	EndIf

	
	;moves
	segment = ReadLine$(file)
	If segment = "moves"
		DebugLog "moves"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id,dx,dy | move,capture | [counter values] | next moves' ids
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment+",," ;bit of a trick
				pos = 0 ;string read position
				m.move = New move
				
				m\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				
				m\dx = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				m\dy = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				m\options[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				m\options[2] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				If Instr(segment, "|", pos+1) - pos > 1 ;if there are counter values

					If Instr(segment, ",", pos+1) > Instr(segment, "|", pos+1) ;if there's only one value
						m\counter[1] = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
						m\counter[0] = 1
					Else ;comma separated list
						While Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							m\counter[ m\counter[0]+1 ] = tempC
							m\counter[0] = m\counter[0]+1
							
							If m\counter[0] > 9
								Exit
							EndIf
						Wend
					EndIf

				EndIf
				
				pos = Instr(segment, "|", pos+1)
				
				While Instr(segment, ",", pos+1) < Len(segment)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					m\nexIds[ m\nexIds[0]+1 ] = tempId
					m\nexIds[0] = m\nexIds[0]+1
					
					If m\nexIds[0] > 999
						Exit
					EndIf
				Wend

			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	;pieces
	segment = ReadLine$(file)
	If segment = "pieces"
		DebugLog "pieces"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id | imgx,imgy,imgsrc | mobile,name$ | moves' ids
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment+",," ;bit of a trick
				pos = 0 ;string read position
				p.piece = New piece
				
				p\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				p\x = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\y = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\img = LoadImage( Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 ) )
				MaskImage p\img, 181, 230, 29
				MidHandle p\img
				
				pos = Instr(segment, "|", pos+1)
				
				;DrawImage p\img, p\x+GraphicsWidth()/2+50*Rand(0,2), p\y+GraphicsHeight()/2
				
				p\mobile = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
				
				pos = Instr(segment, "|", pos+1)
				
				ml1.movelist = New movelist
				While Instr(segment, ",", pos+1) < Len(segment)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					ml2.movelist = New movelist
					ml2\m = fetchMoveById.move(tempId)
					ml2\bef = ml1
					ml1\nex = ml2
					ml1 = ml1\nex
				Wend
				
				While ml1\bef\bef <> Null
					ml1 = ml1\bef
				Wend
				p\m = ml1
				
			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	;cells
	segment = ReadLine$(file)
	If segment = "cells"
		DebugLog "cells"
		cl1.celloplayer = New celloplayer
			
		segment = ReadLine$(file)
		While segment <> ""
		
			;parsing - id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id] | [player(s) id(s)]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				c.cell = New cell
				
				c\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				c\x = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\y = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\img = LoadImage( Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 ) )
				MaskImage c\img, 181, 230, 29
				;MidHandle p\img
				
				pos = Instr(segment, "|", pos+1)
				
				While Instr( segment, ",", Instr(segment, ",", pos+1)+1 ) <> 0 And Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					c\dullhorseIds[ c\dullhorseIds[0]+1 ] = tempId
					c\dullhorseIds[0] = c\dullhorseIds[0]+1
				Wend
				
				pos = Instr(segment, "|", pos+1)
				
				If Instr(segment, "|", pos+1) - pos > 1 ;if there is a piece
					tempId = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
					c\p = fetchPieceById(tempId)
					c\p\c = c
					
					pos = Instr(segment, ",", pos+1)
					c\px = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					c\py = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					
					pos = Instr(segment, "|", pos+1)
					
					If pos+1 < Len(segment) ;if there are players owning this piece

						If Instr(segment, ",", pos+1) = 0 ;if there's only one value
							tempId = Int( Mid$( segment, pos+1, Len(segment)-pos-1 ) )
							c\p\players[1] = fetchPlayerById(tempId)
						Else ;comma separated list
							segment = segment+",," ;bit of a trick
							num = 1
							While Instr(segment, ",", pos+1) < Len(segment)
								tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
								pos = Instr(segment, ",", pos+1)
								
								c\p\players[ num ] = fetchPlayerById(tempId)
								num = num+1
								
								If num > 9
									Exit
								EndIf
							Wend
						EndIf
					
					EndIf
				
				EndIf
				
				cl2.celloplayer = New celloplayer
				cl2\c = c
				cl2\bef = cl1
				cl1\nex = cl2
				cl1 = cl1\nex
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
	Else
		Return 0
	EndIf
	
	While cl1\bef\bef <> Null
		cl1 = cl1\bef
	Wend
	b\cells = cl1
	
	link(b.board) ;turn all ids into pointers
	
	Return 1

End Function

Function export(b.board, path$="")

End Function

Function link(b.board) ;replaces id identifiers with pointer references

	;move -> move
	For m.move = Each move
		ml1.movelist = New movelist
		
		For i = 1 To m\nexIds[0]
		
			ml2.movelist = New movelist
			ml2\m = fetchMoveById( m\nexIds[i] )
			ml2\bef = ml1
			ml1\nex = ml2
			ml1 = ml1\nex
		
		Next
		
		While ml1\bef\bef <> Null
			ml1 = ml1\bef
		Wend
		
		m\nex = ml1
	Next
	
	;cell -> cell
	For c.cell = Each cell
		For i = 1 To c\dullhorseIds[0]
		
			c\dullhorse[i] = fetchCellById( c\dullhorseIds[i] )
		
		Next
	Next

End Function

Function display(b.board)

	cells.celloplayer = b\cells
	While cells <> Null
		DrawImage cells\c\img, cells\c\x, cells\c\y
		
		If cells\c\p <> Null
			DrawImage cells\c\p\img, cells\c\x + cells\c\px, cells\c\y + cells\c\py
		EndIf
		
		cells = cells\nex
	Wend

End Function

Function update(b.board) ;needed?
End Function

Function checkWin(b.board)

End Function

Function undo(b.board, oldb.board) ;implement?
End Function

;piece/cell/move functions
.pieceFuncs
Function choices.celloplayer(p.piece)

End Function

Function movePiece(p.piece, m.move)

End Function

Function possibleCells.celloplayer(c.cell, m.move)

End Function

Function fetchPlayerById.player(id)

	For pl.player = Each player
		If pl\id = id
			Return pl
		EndIf
	Next
	
End Function

Function fetchCellById.cell(id)

	For c.cell = Each cell
		If c\id = id
			Return c
		EndIf
	Next
	
	Return Null

End Function

Function fetchPieceById.piece(id)

	For p.piece = Each piece
		If p\id = id
			Return p
		EndIf
	Next
	
	Return Null

End Function

Function fetchMoveById.move(id)

	For m.move = Each move
		If m\id = id
			Return m
		EndIf
	Next
	
	Return Null

End Function

;mouse functions
.mouseFuncs
Function checkInput(m.mouse)

End Function

;miscellaneous
.miscellFuncs
; navigation ;