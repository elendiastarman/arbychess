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
SeedRnd(MilliSecs())
initialize()
WaitKey
End

; types ;
.types

Type board
	Field cells.celloplayer ;Ha! Pun!
	Field ramifying ;oversimply, whether up then over ends in a different place than over then up
End Type

Type cell
	Field x, y ;for drawing image
	Field px, py ;for positioning piece image
	Field img, alt ;usual image, alt image
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
	;Field vector ;values of 1-9 ; "momentum" direction
	Field options[10] ;flags
	Field counter[10] ;counting number of moves, etc.
	Field nextMoves.movelist ;available next moves
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
	Field selector ;if a cell has been selected (needed?)
	Field hit
	Field down
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
	DebugLog "Ramifying? "+g\b\ramifying

	ClsColor 100, 100, 100
	Cls

	;While Not KeyHit(1)
	;	
	;	Cls
		display(g\b)
	;	Flip
	;	
	;Wend
	
	testcell.cell = fetchCellById(8)
	testpiece.piece = testcell\p
	testcell\alt = LoadImage("cellSelected.png")
	If testcell\alt <> 0
		MaskImage testcell\alt, 1, 1, 1
	EndIf
	
	testdests.celloplayer = possibleCells(testcell, testpiece\m, g\b\ramifying) ;choices(testpiece, g\b\ramifying)
	
	choice.cell = Null
	
	While testdests <> Null
		If testdests\c <> Null
			testdests\c\alt = LoadImage("cellAllow.png")
			If testdests\c\alt <> 0
				MaskImage testdests\c\alt, 1, 1, 1
			EndIf
			
			If choice = Null
				r = Rand(0,1)
				If r = 0
					choice = testdests\c
				EndIf
			EndIf
		EndIf
	
		If testdests\nex <> Null
			testdests = testdests\nex
		Else
			Exit
		EndIf
	Wend
	
	display(g\b)
	
	Delay 500
	
	If choice <> Null
		movePiece(testcell, choice)
	EndIf
	
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
	;  id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id, piece imgx, piece imgy] | [player(s) id(s)]
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
				
				m\options[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;move
				pos = Instr(segment, ",", pos+1)
				m\options[2] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;capture
				
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
		
			;parsing - id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id, piece imgx, piece imgy] | [player(s) id(s)]
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
				
				c\dullhorseIds[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[2] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[3] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\dullhorseIds[4] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
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
	b\ramifying = isRamifying(b\cells)
	
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
		
		m\nextMoves = ml1
	Next
	
	;cell -> cell
	For c.cell = Each cell
		For i = 1 To 4
		
			c\dullhorse[i] = fetchCellById( c\dullhorseIds[i] )
			;DebugLog c\dullhorse[i]\id
		
		Next
	Next

End Function

Function isRamifying(cells.celloplayer)

	;"ramifying" refers to whether order matters
	;That is, if up then over ends in a different place than over then up,
	;then the board is ramifying.
	
	While cells <> Null
	
		For dy = -1 To 1 Step 2
			For dx = -1 To 1 Step 2
			
				dests.celloplayer = destinations(cells\c, dx, dy, 1)
				If dests\nex <> Null ;there are two destinations
					Return 1
				EndIf
				
			Next
		Next
		
		cells = cells\nex
	
	Wend
	
	Return 0

End Function

Function display(b.board)

	cells.celloplayer = b\cells
	While cells <> Null
		DrawImage cells\c\img, cells\c\x, cells\c\y
		
		If cells\c\p <> Null
			DrawImage cells\c\p\img, cells\c\x + cells\c\px, cells\c\y + cells\c\py
		EndIf
		
		If cells\c\alt <> 0
			DrawImage cells\c\alt, cells\c\x, cells\c\y
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
Function choices.celloplayer(p.piece, ramifying)

	Return possibleCells.celloplayer(p\c, p\m, ramifying)

End Function

Function movePiece(start.cell, dest.cell)

	If dest\p <> Null
		dest\p\alive = 0
		dest\p\c = Null
		
		dest\px = start\px
		dest\py = start\py
		dest\p = start\p
		start\p = Null
	Else
		dest\px = start\px
		dest\py = start\py
		dest\p = start\p
		start\p = Null
	EndIf

End Function

Function possibleCells.celloplayer(c.cell, moves.movelist, ramifying)

	;for each move, destination cells
	;for each destination cell, recurse
	
	final.celloplayer = New celloplayer
	
	While moves <> Null
	
		;step-1 cells
		dests.celloplayer = destinations.celloplayer(c, moves\m\dx, moves\m\dy, ramifying)
		
		While dests <> Null ;iterate through destination cells
			If dests\c <> Null
			
				If cellInList(final, dests\c) = 0
					If (moves\m\options[1] > 0 And dests\c\p = Null) Or (moves\m\options[2] > 0 And dests\c\p <> Null) ;move or capture
						appendCell(final, dests\c)
					EndIf
				EndIf
			
				If (moves\m\options[1] = 2 And dests\c\p = Null) Or (moves\m\options[2] = 2 And dests\c\p <> Null) ;if chaining is allowed
					dests2.celloplayer = possibleCells.celloplayer(dests\c, moves\m\nextMoves, ramifying)
					
					;;add to end of dests but don't include repeats;;
					While dests2 <> Null
						If dests2\c <> Null
						
							If cellInList(final, dests2\c) = 0
								appendCell(final, dests2\c)
							EndIf
							
						EndIf
						
						If dests2\nex <> Null
							dests2 = dests2\nex
						Else
							Exit
						EndIf
					Wend
					;;
					
					If cellInList(final, dests\c) = 0
						appendCell(final, dests\c)
					EndIf
				EndIf
				
			EndIf
			
			If dests\nex <> Null ;end condition
				dests = dests\nex
			Else
				Exit
			EndIf
		Wend
	
		If moves\nex <> Null
			moves = moves\nex
		Else
			Exit
		EndIf
	Wend
	
	While moves\bef <> Null ;rewind moves list
		moves = moves\bef
	Wend
	
	While final\bef <> Null ;rewind cell list
		final = final\bef
	Wend
	
	Return final

End Function

Function destinations.celloplayer(c.cell, dx, dy, checkAlt=0)

	;rationale for choosing to check only a zig-zag pattern:
	
	;- Checking every route that consists of |dx| steps in the
	;x direction and |dy| steps in the y direction scales poorly.
	;The formula is Binom(|dx|+|dy|, |dx|). Max value is reached
	;when |dx| = |dy|, and when |dx|=|dy|=9, Binom(18,9) > 2^31-1,
	;which is the largest value that a Blitz int can hold.
	
	;- Another possibility is checking all semi-zig-zag patterns,
	;that is, patterns where no more than 2 steps are taken in
	;either the x or y directions before changing direction.
	;The formula for that is 2^(min(|dx|,|dy|)), which scales
	;better than checking every route, but still poorly. It also
	;has an upper bound like the previous method.
	
	;- The two remaining sensible and simple options are to take
	;the sides of a rectangle (do all of dx first, then all of dy)
	;or to zig-zag (alternate dx and dy). When considering straight
	;diagonal moves (|dx| ~ |dy|), zig-zagging is closer to the
	;spirit of a diagonal move. Thus, the same spirit shall be
	;applied to non-purely diagonal moves where dx and dy do not
	;have approximately the same magnitude.
	
	;Hence, in the case where order matters, x and y steps shall
	;be chosen so as to remain as close as possible to the straight
	;line connecting (0,0) to (dx,dy).
	
	;tl;dr - diagonal moves will be checked as close to diagonal as possible.
	;At most 2 cells will be returned from this function.
	
	x = 0
	y = 0
	sx = Sgn(dx)
	sy = Sgn(dy)
	
	dests.celloplayer = New celloplayer
	curr.cell = c
	
	; x| y| cell neighbor
	;  |-1| 1 (north)
	;-1|  | 4 (west)
	;  | 1| 3 (south)
	; 1|  | 2 (east)
	
	While x <> dx Or y <> dy
	
		If Abs( (x+sx)*dy ) < Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=1)
		
			If curr\dullhorse[3-sx] = Null
				Exit
			Else
				curr = curr\dullhorse[3-sx]
				x = x+sx
			EndIf
			
		ElseIf Abs( (x+sx)*dy ) > Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=0)
		
			If curr\dullhorse[2+sy] = Null
				Exit
			Else
				curr = curr\dullhorse[2+sy]
				y = y+sy
			EndIf
			
		EndIf
	
	Wend
	
	If x = dx And y = dy
		dests\c = curr
	EndIf
		
	If checkAlt = 1 ;if board is ramifying, so order matters, so check the other route
		dests2.celloplayer = destinations.celloplayer(c, dx, dy, 0)
		
		If dests2\c <> Null ;alternate found
			If dests\c = Null ;original not found
				dests\c = dests2\c ;"replace"
			ElseIf dests2\c <> dests\c ;different cells
				dests\nex = dests2 ;add to list
				dests2\bef = dests
			EndIf
		EndIf
	EndIf
	
	Return dests

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

Function cellInList(cl.celloplayer, c.cell)

	found = 0
	
	If cl\c = c
		Return 1
	EndIf
	
	hold.cell = cl\c
	
	While cl\bef <> Null
		If cl\c = c
			found = 1
			Exit
		EndIf
		
		cl = cl\bef
	Wend
	
	While cl\c <> hold
		cl = cl\nex
	Wend
	
	If found = 0
		While cl <> Null
			If cl\c = c
				found = 1
				Exit
			EndIf
		
			If cl\nex <> Null
				cl = cl\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While cl\c <> hold
		cl = cl\bef
	Wend
	
	Return found

End Function

Function moveInList(ml.movelist, m.move)

	found = 0
	
	If ml\m = m
		Return 1
	EndIf
	
	hold.move = ml\m
	
	While ml\bef <> Null
		If ml\m = m
			found = 1
			Exit
		EndIf
		
		ml = ml\bef
	Wend
	
	While ml\m <> hold
		ml = ml\nex
	Wend
	
	If found = 0
		While ml <> Null
			If ml\m = m
				found = 1
				Exit
			EndIf
		
			If ml\nex <> Null
				ml = ml\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While ml\m <> hold
		ml = ml\bef
	Wend
	
	Return found

End Function

Function appendCell(cl.celloplayer, c.cell)

	hold.cell = cl\c

	While cl <> Null
		If cl\nex <> Null
			cl = cl\nex
		Else
			Exit
		EndIf
	Wend
	
	cl\nex = New celloplayer
	cl\nex\c = c
	cl\nex\bef = cl
	
	While cl\c <> hold
		cl = cl\bef
	Wend

End Function

Function appendMove(ml.movelist, m.move)

	hold.move = ml\m

	While ml <> Null
		If ml\nex <> Null
			ml = ml\nex
		Else
			Exit
		EndIf
	Wend
	
	ml\nex = New movelist
	ml\nex\m = m
	ml\nex\bef = ml
	
	While ml\m <> hold
		ml = ml\bef
	Wend

End Function

; navigation ;