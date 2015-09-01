;;;
;
; Author: Lee Burnette/El'endia Starman
; Date: 2014-01-14
;
; Name: ArbyChess
; Function: A chess engine that allows arbitrary boards and piece moves
;
;;;

; ; graphics selector goes here ; ;
.Graphics

Graphics 800, 600, 32, 2
SeedRnd(MilliSecs())
initialize()
WaitKey
End

; types ;
.types

Type board
	Field cells.celloplayer ;Ha! Pun!
	Field prevMoves.historyList
	Field specials.specialsList
	Field images.imageList
	Field players.playerList
	Field current.playerList
	Field ramifying ;oversimply, whether up then over ends in a different place than over then up
End Type

Type cell
	Field x, y ;for drawing image
	Field px, py ;for positioning piece image
	Field img, alt ;usual image, alt image
	Field dullhorse.cell[4] ;Ha! Another pun!  ;1=north, 2=east, 3=south, 4=west
	Field dullhorseIds[4]
	Field p.piece
	Field id
	Field visited
End Type

Type piece
	Field x, y ;for drawing image
	Field img
	Field m.movelist ;available moves
	Field mobile
	Field vulnerable
	Field name$
	Field id
	Field players.playerlist ;allow the possibility of multiple players controlling the same piece
	;Field ...
End Type

Type move
	Field dx, dy
	;Field vector ;values of 1-9 ; "momentum" direction
	Field options[10] ;flags
	Field counter[10] ;counting number of moves, etc.
	Field nextMoves.movelist ;available subsequent moves
	Field nexIds[1000];
	Field chainMoves.movelist ;available chained moves
	Field chainIds[1000]
	Field id
End Type

Type special
	Field name$
	Field params[1000]
End Type

Type movelist
	Field m.move
	Field bef.movelist, nex.movelist ;doubly-linked list
End Type

Type celloplayer
	Field c.cell
	Field bef.celloplayer, nex.celloplayer ;doubly-linked list
End Type

Type cellMoveList
	Field c.cell
	Field mv.move
	Field srcmv.move
	Field bef.cellMoveList, nex.cellMoveList
End Type

Type playerlist
	Field p.player
	Field bef.playerlist, nex.playerlist
End Type

Type historyList
	Field m.move ;move used
	Field p.piece ;piece moved
	Field srcCell.cell, destCell.cell ;cell moved from and to
	Field capId, capX, capY, capCell.cell, capP.piece ;capId = id of piece that was captured; 0 if no piece was captured
	Field prevmvs.moveList, capprevmvs.moveList
	Field playa.playerList
	Field other.historyList ;for moves like castling, where there are two (or more) pieces to put back
	Field bef.historyList, nex.historyList
End Type

Type specialsList
	Field s.special
	Field bef.specialsList, nex.specialsList
End Type

Type imageList
	Field id
	Field name$
	Field filename$
	Field image
	Field bef.imageList, nex.imageList
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
	
	;success = import(b, "hex.txt") ;for now, the simple game called Hex-a-pawn
	success = import(b, "chess.txt") ;gonna try it with chess
	
	If success
		g\b\prevMoves = New historyList
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
	
	SetBuffer(BackBuffer())
	
	selected.cell = Null
	allowed.cellMoveList = Null
	g\b\current = g\b\players
	
	DebugLog "Current player: "+g\b\current\p\name+", id: "+g\b\current\p\id

	While Not KeyHit(1)
	
		If checkInput(g\m)
		
			If g\m\hit = 2 ;undo
				undo(g\b)
				g\m\selector = 0
				clearCellMoveList(allowed)
				clearCellAlts()
				highlightVulnerable(g\b, 5)
			EndIf
			
			If g\m\selector = 1 And g\m\hit = 1 ;first selection
			
				checkAllSpecials(g\b, 0)
			
				If g\m\c <> Null
				
					If g\m\c\p <> Null
						If playerInList(g\m\c\p\players, g\b\current\p)
						
							selected = g\m\c
							
							g\m\c\alt = fetchImageById(g\b\images, 1)
							
							allowed = possibleCells(g\m\c, g\m\c\p\m, g\b\ramifying, g\b\current\p)
							
							While allowed <> Null
								If allowed\c <> Null
									
									allowed\c\alt = fetchImageById(g\b\images, 2)
									valid = 1
									valid = valid * checkConditions(g\b, allowed, "castling")
									
									If valid = 1 ;check for out of check
									
										hold.piece = allowed\c\p
										allowed\c\p = selected\p
										selected\p = Null
										
										vuln.celloplayer = checkVulnerable(g\b)
										
										If vuln <> Null
											valid = 0
											While vuln <> Null
												If vuln\nex <> Null
													vuln = vuln\nex
													Delete vuln\bef
												Else
													Delete vuln
												EndIf
											Wend
										EndIf
										
										selected\p = allowed\c\p
										allowed\c\p = hold
										hold = Null
									
									EndIf
									
									If valid = 0 ;validation check failed
										allowed\c\alt = 0
									EndIf
									
								EndIf
								
								If allowed\c\alt = 0 ;need to delete allowed but keep the list traversable
								
									If allowed\nex <> Null ;if there is a next move
										
										If allowed\bef <> Null
											allowed\nex\bef = allowed\bef
											allowed = allowed\nex
											Delete allowed\bef\nex
											allowed\bef\nex = allowed
										Else
											allowed = allowed\nex
											Delete allowed\bef
										EndIf
										
									Else ;no next move
									
										If allowed\bef <> Null ;previous available
											allowed = allowed\bef
											Delete allowed\nex
										Else ;no previous either
											Delete allowed
										EndIf
										Exit
									
									EndIf
									
								Else
								
									If allowed\nex <> Null ;need to stop on the last one so the list can still be accessed when actually moving
										allowed = allowed\nex
									Else
										Exit
									EndIf
									
								EndIf
							Wend
						
						Else
							g\m\selector = 0 ;reset if selected doesn't belong to current player
						EndIf
						
					Else
						g\m\selector = 0 ;reset if empty square selected
					EndIf
				EndIf
				
			ElseIf g\m\selector = 2 Or g\m\selector = 3

				If g\m\selector = 2 ;second selection
					If g\m\c <> Null
						cml.cellMoveList = cellInList_cml(allowed, g\m\c)
						If cml <> Null
							hist.historyList = g\b\prevMoves
							hist\nex = New historyList
							hist\nex\bef = hist
							hist = hist\nex
							
							movePiece(selected, g\m\c, hist)
							
							If hist <> Null
								hist\m = cml\srcmv
								hist\prevmvs = g\m\c\p\m
								hist\playa = g\b\current
							EndIf
							
							g\b\prevMoves = g\b\prevMoves\nex
							
							g\m\c\p\m = cml\srcmv\nextMoves
							
							checkAllSpecials(g\b, 0)
							
							;garbage-collect -allowed-
							While allowed\bef <> Null
								allowed = allowed\bef
							Wend
							While allowed <> Null
								If allowed\nex = Null
									Delete allowed
								Else
									allowed = allowed\nex
									Delete allowed\bef
								EndIf
							Wend
							Delete cml
							
							g\b\current = g\b\current\nex
							DebugLog "Current player: "+g\b\current\p\name+", id: "+g\b\current\p\id
						EndIf
					EndIf
				EndIf
				
				selected = Null
				clearCellAlts()
				highlightVulnerable(g\b, 5)
				g\m\selector = 0
				
			EndIf
		
		EndIf
		
		Cls
		display(g\b)
		Flip
		
	Wend
	
	display(g\b)

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
	
	For hl.historyList = Each historyList
		Delete hl
	Next
	DebugLog "Historylist cleared."
	
	For cm.cellMoveList = Each cellMoveList
		Delete cm
	Next
	DebugLog "Cell move lists cleared."
	
	For sp.specialsList = Each specialsList
		Delete sp\s
		Delete sp
	Next
	DebugLog "Specials deleted."
	
	For im.imageList = Each imageList
		FreeImage im\image
		Delete im
	Next
	DebugLog "Images deleted."
	
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
	;  id,dx,dy | move,capture[,non-jumping] | [counter values] | [chained moves' ids] | next moves' ids
	;  ""
	;  "pieces"
	;  id | imgx,imgy,imgsrc | mobile,vulnerable,name$ | moves' ids
	;  ""
	;  "specials"
	;  name$ | [params]
	;  ""
	;  "cell images"
	;  id | name$,filename$ [| maskR, maskG, maskB]
	;  ""
	;  "cells"
	;  id | imgx,imgy,imgsrc | northId,eastId,southId,westId | [piece id, piece imgx, piece imgy] | [player(s) id(s)]
	;  ""
	;  "promotion"
	;  promotable id | promote-to id(s) | promotion cell id(s)
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
		
		b\players = New playerList
		
		While segment <> "" And Eof(file)=0
		
			;parsing - id | name$ | [counter values]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				pl.player = New player
				
				If b\players\p <> Null
					b\players\nex = New playerList
					b\players\nex\bef = b\players
					b\players = b\players\nex
				EndIf
				
				pl\id = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				pl\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
					
				pos = Instr(segment, "|", pos+1)
				
				If pos+1 <= Len(segment) ;if there is a counter value

					If Instr(segment, ",", pos+1) = 0 ;if there's only one value
						pl\counter[1] = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
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
				
				b\players\p = pl
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
		
		temp.playerList = b\players
		While temp\bef <> Null
			temp = temp\bef
		Wend
		temp\bef = b\players
		b\players\nex = temp
		b\players = b\players\nex
	
	Else
		Return 0
	EndIf

	
	;moves
	segment = ReadLine$(file)
	If segment = "moves"
		DebugLog "moves"
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - id,dx,dy | move,capture[,non-jumping] | [counter values] | [chained moves' ids] | next moves' ids
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
				If Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1) ;meaning there's a third number
					pos = Instr(segment, ",", pos+1)
					m\options[3] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) ) ;non-jumping
				EndIf
				
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
				If Instr(segment, "|", pos+1) - pos > 1 ;if there are chained move ids

					If Instr(segment, ",", pos+1) > Instr(segment, "|", pos+1) ;if there's only one value
						m\chainIds[1] = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
						m\chainIds[0] = 1
					Else ;comma separated list
						While Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							m\chainIds[ m\chainIds[0]+1 ] = tempC
							m\chainIds[0] = m\chainIds[0]+1
							
							If m\chainIds[0] > 999
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
				
				p\mobile = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				p\vulnerable = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
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
	
	;specials
	segment = ReadLine$(file)
	If segment = "specials"
		DebugLog "specials"
		b\specials = New specialsList
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - name$ | [counter values]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				pos = 0 ;string read position
				spec.special = New special
				
				spec\name = Mid$( segment, pos+2, Instr(segment, "|", pos+1)-pos-3 )
					
				pos = Instr(segment, "|", pos+1)
				
				If pos+1 <= Len(segment) ;if there is a counter value

					If Instr(segment, ",", pos+1) = 0 ;if there's only one value
						spec\params[1] = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
						spec\params[0] = 1
					Else ;comma separated list
						segment = segment+",," ;bit of a trick
						While Instr(segment, ",", pos+1) < Len(segment)
							tempC = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
							pos = Instr(segment, ",", pos+1)
							
							spec\params[ spec\params[0] ] = tempC
							spec\params[0] = spec\params[0]+1
							
							If spec\params[0] > 999
								Exit
							EndIf
						Wend
					EndIf
				
				EndIf
				
				If b\specials\s <> Null
					b\specials\nex = New specialsList
					b\specials\nex\bef = b\specials
					b\specials = b\specials\nex
				EndIf
				
				b\specials\s = spec
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
		
		If b\specials\s = Null
			Delete b\specials
		Else
			While b\specials\bef <> Null
				b\specials = b\specials\bef
			Wend
		EndIf
	
	Else
		Return 0
	EndIf
	
	;cell images
	segment = ReadLine$(file)
	If segment = "cell images"
		DebugLog "cell images"
		imgList.imageList = New imageList
		
		segment = ReadLine$(file)
		While segment <> ""
		
			;parsing - id | name$,filename$ [| maskR, maskG, maskB]
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment + "," ;bit of a trick
				pos = 0 ;string read position
				
				If imgList\id <> 0
					imgList\nex = New imageList
					imgList\nex\bef = imgList
					imgList = imgList\nex
				EndIf
				
				imgList\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				imgList\name = Mid$( segment, pos+2, Instr(segment, ",", pos+1)-pos-3 )
				pos = Instr(segment, ",", pos+1)
				imgList\filename = Mid$( segment, pos+2, Instr(segment, Chr(34), pos+2)-pos-2 )
				
				imgList\image = LoadImage(imgList\filename)
				
				If imgList\image <> 0
					pos = Instr(segment, "|", pos+1)
					If pos <> 0 ;there are masking parameters
						maskR = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
						pos = Instr(segment, ",", pos+1)
						maskG = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
						pos = Instr(segment, ",", pos+1)
						maskB = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
						
						MaskImage imgList\image, maskR, maskG, maskB
						DebugLog maskR+","+maskG+","+maskB
					EndIf
				EndIf
			EndIf
			
			segment = ReadLine$(file)
		Wend
		
		While imgList\bef <> Null
			imgList = imgList\bef
		Wend
		
		b\images = imgList
		
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
				
				If cl1\c <> Null
					cl1\nex = New celloplayer
					cl1\nex\bef = cl1
					cl1 = cl1\nex
				EndIf
				
				;cl1\c = c
				
				c\id = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				
				c\x = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\y = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				pos = Instr(segment, ",", pos+1)
				c\img = fetchImageById( b\images, Int(Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 )) )
				MaskImage c\img, 181, 230, 29
				
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
					c\p = copyPiece( fetchPieceById(tempId) )
					c\p\players = New playerlist
					
					pos = Instr(segment, ",", pos+1)
					c\px = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					c\py = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					
					pos = Instr(segment, "|", pos+1)
					
					If pos+1 <= Len(segment) ;if there are players owning this piece
					
						If Instr(segment, ",", pos+1) = 0 ;if there's only one value
							tempId = Int( Mid$( segment, pos+1, Len(segment)-pos ) )
							c\p\players\p = fetchPlayerById(tempId)
						Else ;comma separated list
							segment = segment+",," ;bit of a trick

							While Instr(segment, ",", pos+1) < Len(segment)
								tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
								pos = Instr(segment, ",", pos+1)
								
								If c\p\players = Null
									c\p\players\p = fetchPlayerById(tempId)
								Else
									appendPlayer(c\p\players, fetchPlayerById(tempId))
								EndIf
							Wend
						EndIf
						
						DebugLog c\p\players\p\name
					
					EndIf
				
				EndIf
				
				cl1\c = c
			
			EndIf
			
			segment = ReadLine$(file)
		Wend
	
		While cl1\bef <> Null
			cl1 = cl1\bef
		Wend
		b\cells = cl1
		
	Else
		Return 0
	EndIf
	
	;promotion
	segment = ReadLine$(file)
	If segment = "promotion"
		DebugLog "promotion"
		
		If b\specials <> Null
			While b\specials\nex <> Null
				b\specials = b\specials\nex
			Wend
		Else
			b\specials = New specialsList
		EndIf
		
		segment = ReadLine$(file)
		While segment <> "" And Eof(file)=0
		
			;parsing - promotable id | promote-to id(s) | promotion cell id(s)
			If Mid$(segment, 1, 1) <> ";" ;exclude comments
				DebugLog segment
				segment = segment+",," ;bit of a trick
				segment = Replace(segment, "|", ",|") ;another trick to make the loop work

				pos = 0 ;string read position
				s.special = New special
				
				s\name = "promotion"
				s\params[0] = 1
				
				;id
				s\params[1] = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
				
				pos = Instr(segment, "|", pos+1)
				offset = s\params[0]+1
				
				;promote-to id(s)
				If Instr(segment, ",", pos+1) > Instr(segment, "|", pos+1) ;if there's only one value
					s\params[offset+1] = Int( Mid$( segment, pos+1, Instr(segment, "|", pos+1)-pos-1 ) )
					s\params[offset] = 1
				Else ;comma separated list
					While Instr(segment, ",", pos+1) < Instr(segment, "|", pos+1)
						tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
						pos = Instr(segment, ",", pos+1)
						
						s\params[ offset+s\params[offset]+1 ] = tempId
						s\params[offset] = s\params[offset]+1
						
						If s\params[offset] > 999-1
							Exit
						EndIf
					Wend
				EndIf
				
				offset = offset + s\params[offset] + 1
				
				pos = Instr(segment, "|", pos+1)
				
				;promotion cell id(s)
				While Instr(segment, ",", pos+1) < Len(segment)
					tempId = Int( Mid$( segment, pos+1, Instr(segment, ",", pos+1)-pos-1 ) )
					pos = Instr(segment, ",", pos+1)
					
					s\params[offset+s\params[offset]+1] = tempId
					s\params[offset] = s\params[offset]+1
					
					If offset+s\params[offset] > 999-1
						Exit
					EndIf
				Wend
				
				If b\specials\s <> Null
					b\specials\nex = New specialsList
					b\specials\nex\bef = b\specials
					b\specials = b\specials\nex
				EndIf
				
				b\specials\s = s
				
			EndIf
			
			segment = ReadLine$(file)
		Wend
		
		If b\specials\s = Null
			Delete b\specials
		Else
			While b\specials\bef <> Null
				b\specials = b\specials\bef
			Wend
		EndIf
		
	Else
		Return 0
	EndIf
	
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
		
		If m\chainIds[0] > 0
			ml3.movelist = New movelist
			
			For j = 1 To m\chainIds[0]
			
				ml4.movelist = New movelist
				ml4\m = fetchMoveById( m\chainIds[j] )
				ml4\bef = ml3
				ml3\nex = ml4
				ml3 = ml3\nex
			
			Next
			
			While ml3\bef\bef <> Null
				ml3 = ml3\bef
			Wend
			
			m\chainMoves = ml3
		EndIf
		
	Next
	
	;cell -> cell
	For c.cell = Each cell
		For i = 1 To 4
		
			c\dullhorse[i] = fetchCellById( c\dullhorseIds[i] )
		
		Next
	Next

End Function

Function isRamifying(cells.celloplayer)

	;"ramifying" refers to whether order matters
	;That is, if up then over ends in a different place than over then up,
	;then the board is ramifying.
	
	ramifying = 0
	
	While cells <> Null And ramifying = 0
	
		For dy = -1 To 1 Step 2
			For dx = -1 To 1 Step 2
			
				dests.celloplayer = destinations(cells\c, dx, dy, 1)
				If dests\nex <> Null ;there are two destinations
					ramifying = 1
					Delete dests\nex
				EndIf
				Delete dests
				
			Next
		Next
		
		cells = cells\nex
	
	Wend
	
	Return ramifying

End Function

Function display(b.board)

	cells.celloplayer = b\cells
	While cells <> Null
		DrawImage cells\c\img, cells\c\x, cells\c\y
		
		If cells\c\p <> Null ;if the cell has a piece on it
			DrawImage cells\c\p\img, cells\c\x + cells\c\px, cells\c\y + cells\c\py
		EndIf
		
		If cells\c\alt <> 0 ;if the cell is selected or move-to-able
			DrawImage cells\c\alt, cells\c\x, cells\c\y
		EndIf
		
		cells = cells\nex
	Wend
	
	Text 20,20,b\current\p\name

End Function

Function checkWin(b.board)

End Function

Function undo(b.board)

	hist.historyList = b\prevMoves

	If hist\bef <> Null
		
		;if any, take care of the other moves too (like the rook move of castling)
		While hist\other <> Null

			hist\other\srcCell\px = hist\other\destCell\px
			hist\other\srcCell\py = hist\other\destCell\py
			hist\other\srcCell\p  = hist\other\destCell\p
			hist\other\destCell\p = Null
			
			If hist\other\capId > 0 ;supposing that other pieces were captured as well and need to be restored...
				hist\other\capCell\p = hist\other\capP
				hist\other\capCell\px = hist\other\capx
				hist\other\capCell\py = hist\other\capy
				hist\other\capCell\p\m = hist\other\capprevmvs
			EndIf
			
			hist\other = hist\other\nex
		Wend
		
		;move piece back
		hist\srcCell\px = hist\destCell\px
		hist\srcCell\py = hist\destCell\py
		hist\srcCell\p  = hist\destCell\p
		hist\srcCell\p\m = hist\prevmvs
		hist\destCell\p = Null
		
		;restore captured piece, if any
		If hist\capId > 0
			hist\capCell\p = hist\capP
			hist\capCell\px = hist\capx
			hist\capCell\py = hist\capy
			hist\capCell\p\m = hist\capprevmvs
		EndIf
		
		;rewind turn to previous player
		b\current = hist\playa
		
		;move current history back
		hist = hist\bef
		b\prevMoves = b\prevMoves\bef
		
		checkAllSpecials(b, 1)
		
	EndIf
	
End Function

;piece/cell/move functions
.partFuncs

Function movePiece(start.cell, dest.cell, hist.historyList)

	If dest\p <> Null
		hist\capId = dest\p\id
		hist\capP = dest\p
		hist\capx = dest\px
		hist\capy = dest\py
		hist\capprevmvs = dest\p\m
		hist\capCell = dest
	EndIf
	
	hist\srcCell = start
	hist\destCell = dest
	hist\p = start\p
	
	dest\px = start\px
	dest\py = start\py
	dest\p = start\p
	start\p = Null

End Function

Function possibleCells.cellMoveList(c.cell, moves.movelist, ramifying, playa.player)

	For c2.cell = Each cell ;force precondition: cells have not been visited before
		c2\visited = 0
	Next
	
	c\visited = 1

	;for each move, destination cells
	;for each destination cell, "recurse"
	
	final.cellMoveList = New cellMoveList
	final\c = c
	final\mv = moves\m
	moves = moves\nex
	
	While moves <> Null
		final\nex = New cellMoveList
		final\nex\c = c
		final\nex\mv = moves\m
		final\nex\bef = final
		final = final\nex
	
		moves = moves\nex
	Wend
	
	fin.cellMoveList = final
	While fin\bef <> Null
		fin = fin\bef
	Wend

	dests1.celloplayer = Null
	dests2.celloplayer = Null
	;dests3.celloplayer = Null
	
	While fin <> Null
	
		If fin\mv <> Null
		
			;find destinations with current move
			dests1 = destinations(fin\c, fin\mv\dx, fin\mv\dy, ramifying, fin\mv\options[3]) ;get destination(s)
			
			While dests1 <> Null
				If dests1\c <> Null
					If dests1\c\visited = 0
						allow = 0
					
						op1 = fin\mv\options[1] ;0 = can't move, 1 = move once, 2 = chain, 3 = branch
						op2 = fin\mv\options[2] ;same, but with capture
						
						If op1 >= 1 And dests1\c\p = Null
							allow = op1
						ElseIf op2 >= 1 And dests1\c\p <> Null
							If playerInList(dests1\c\p\players, playa) = 0
								allow = op2
							EndIf
						EndIf
						
						If allow >= 1
							final\nex = New cellMoveList
							final\nex\c = dests1\c
							dests1\c\visited = 1
							
							If allow = 1
								final\nex\mv = Null
								final\nex\srcmv = fin\mv
							ElseIf allow = 2
								final\nex\mv = fin\mv
								final\nex\srcmv = fin\mv
							EndIf
							
							final\nex\bef = final
							final = final\nex
						EndIf
						
					EndIf
				EndIf
				
				;garbage-collect -dests1-
				If dests1\nex = Null
					Delete dests1
				Else
					dests1 = dests1\nex
					Delete dests1\bef
				EndIf
			Wend
			
			;find destinations with chain moves
			While fin\mv\chainMoves <> Null
	
				dests2 = destinations(fin\c, fin\mv\chainMoves\m\dx, fin\mv\chainMoves\m\dy, ramifying, fin\mv\chainMoves\m\options[3]) ;get destination(s)
				
				While dests2 <> Null ;loop through destinations
					If dests2\c <> Null
						If dests2\c\visited = 0 ;if I haven't been there before
							allow = 0
						
							op1 = fin\mv\chainMoves\m\options[1] ;0 = can't move, 1 = move once, 2 = chain, 3 = branch
							op2 = fin\mv\chainMoves\m\options[2] ;same, but with capture
							
							If op1 > 0 And dests2\c\p = Null
								allow = op1
							ElseIf op2 > 0 And dests2\c\p <> Null
								If playerInList(dests2\c\p\players, playa)=0
									allow = op1
								EndIf
							EndIf
							
							If allow >= 1
								final\nex = New cellMoveList
								final\nex\c = dests2\c
								dests2\c\visited = 1
								
								If allow = 1
									final\nex\mv = Null
									final\nex\srcmv = fin\mv\chainMoves\m
								ElseIf allow = 2
									final\nex\mv = fin\mv\chainMoves\m
									final\nex\srcmv = fin\mv\chainMoves\m
								EndIf

								final\nex\bef = final
								final = final\nex
							EndIf
							
						EndIf
					EndIf
					
					;garbage-collect -dests2-
					If dests2\nex = Null
						Delete dests2
					Else
						dests2 = dests2\nex
						Delete dests2\bef
					EndIf
				Wend
				
				fin\mv\chainMoves = fin\mv\chainMoves\nex
				
			Wend
		EndIf
	
		fin = fin\nex
	
	Wend
	
	While final\bef <> Null ;rewind cell list	
		final = final\bef
	Wend
	
	While final\nex <> Null And final\c = c
		final = final\nex
		Delete final\bef
		
		If final\nex = Null
			If final\c = c
				Delete final
				Return Null
			Else
				Return final
			EndIf
		EndIf
	Wend

	Return final

End Function

Function destinations.celloplayer(c.cell, dx, dy, checkAlt=0, nonjumping=0)

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
	
		If Abs( (x+sx)*dy ) < Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=1) Or dy=0
		
			If curr\dullhorse[3-sx] = Null
				Exit
			Else
				curr = curr\dullhorse[3-sx]
				
				If nonjumping = 1
					If curr\p <> Null
						Exit
					EndIf
				EndIf
				
				x = x+sx
			EndIf
			
		ElseIf Abs( (x+sx)*dy ) > Abs( (y+sy)*dx ) Or (Abs( (x+sx)*dy ) = Abs( (y+sy)*dx ) And checkAlt=0)
		
			If curr\dullhorse[2+sy] = Null
				Exit
			Else
				curr = curr\dullhorse[2+sy]
				
				If nonjumping = 1
					If curr\p <> Null
						Exit
					EndIf
				EndIf
				
				y = y+sy
			EndIf
			
		EndIf
	
	Wend
	
	If x = dx And y = dy
		dests\c = curr
	EndIf
		
	If checkAlt = 1 ;if board is ramifying, order matters, so check the other route
		dests2.celloplayer = destinations.celloplayer(c, dx, dy, 0, nonjumping)
		
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

Function playerInList(pl.playerlist, p.player)

	If pl = Null
		Return 0
	EndIf

	found = 0
	
	If pl\p = p
		Return 1
	EndIf
	
	hold.player = pl\p
	
	While pl\bef <> Null
		If pl\p = p
			found = 1
			Exit
		EndIf
		
		pl = pl\bef
	Wend
	
	While pl\p <> hold
		pl = pl\nex
	Wend
	
	If found = 0
		While pl <> Null
			If pl\p = p
				found = 1
				Exit
			EndIf
		
			If pl\nex <> Null
				pl = pl\nex
			Else
				Exit
			EndIf
		Wend
	EndIf
	
	While pl\p <> hold
		pl = pl\bef
	Wend
	
	Return found

End Function

Function appendPlayer(pl.playerlist, p.player)

	If pl = Null
		pl = New playerlist
		pl\p = p
		Return
	EndIf

	hold.player = pl\p

	While pl <> Null
		If pl\nex <> Null
			pl = pl\nex
		Else
			Exit
		EndIf
	Wend
	
	pl\nex = New playerlist
	pl\nex\p = p
	pl\nex\bef = pl
	
	While pl\p <> hold
		pl = pl\bef
	Wend

End Function

Function fetchCellById.cell(id)

	For c.cell = Each cell
		If c\id = id
			Return c
		EndIf
	Next
	
	Return Null

End Function

Function cellInList(cl.celloplayer, c.cell)

	If cl = Null
		Return 0
	EndIf

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

Function cellInList_cml.cellMoveList(cl.cellMoveList, c.cell)

	If cl = Null
		Return Null
	EndIf

	If cl\c = c
		Return cl
	EndIf

	While cl\bef <> Null
		cl = cl\bef
		If cl\c = c
			Return cl
		EndIf
	Wend
	
	While cl <> Null
		If cl\c = c
			Return cl
		EndIf
		cl = cl\nex
	Wend

	Return Null
	
End Function

Function appendCell(cl.celloplayer, c.cell)

	If cl = Null
		cl = New celloplayer
		cl\c = c
		Return
	EndIf

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

Function fetchPieceById.piece(id)

	For p.piece = Each piece
		If p\id = id
			Return p
		EndIf
	Next
	
	Return Null

End Function

Function copyPiece.piece(p.piece)

	cp.piece = New piece
	
	cp\x = p\x
	cp\y = p\y
	cp\img = p\img
	cp\m = p\m
	cp\mobile = p\mobile
	cp\vulnerable = p\vulnerable
	cp\name = p\name
	cp\id = p\id
	cp\players = p\players
	
	Return cp

End Function

Function fetchMoveById.move(id)

	For m.move = Each move
		If m\id = id
			Return m
		EndIf
	Next
	
	Return Null

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

Function fetchImageById(imgList.imageList, id)

	image = 0

	While imgList <> Null
		If imgList\id = id
			image = imgList\image
			Exit
		EndIf
		
		If imgList\nex = Null
			Exit
		Else
			imgList = imgList\nex
		EndIf
	Wend
	
	While imgList\bef <> Null
		imgList = imgList\bef
	Wend
	
	Return image

End Function

Function removeMove(ml.moveList, moveId)

	;assumption: ml is at the beginning of the move list

	If ml\m\id = moveId ;if it's the first item
		If ml\nex <> Null
			ml = ml\nex
			Delete ml\bef
		Else
			Delete ml
		EndIf
		Return 1
	EndIf
	
	success = 0
	
	While ml\nex <> Null
		If ml\m\id = moveId
			;we know this is neither the first item nor the last
			ml\bef\nex = ml\nex
			ml\nex\bef = ml\bef
			success = 1
		EndIf
		ml = ml\nex
	Wend
	
	If ml\m\id = moveId ;if it's the last item
		ml = ml\bef
		ml\nex = Null
		success = 1
	EndIf
	
	;end condition: ml is at the beginning of the modified list
	While ml\bef <> Null
		ml = ml\bef
	Wend
	
	Return success

End Function

Function clearCellAlts()

	For c.cell = Each cell
		c\alt = 0
	Next

End Function

Function clearCellMoveList(cml.cellMoveList)

	While cml <> Null
		If cml\nex <> Null
			cml = cml\nex
			Delete cml\bef
		Else
			Delete cml
		EndIf
	Wend

End Function

;mouse functions
.mouseFuncs
Function checkInput(m.mouse)

	change = 0
	m\hit = 0

	If MouseHit(1)
		m\hit = 1
		
		If m\selector = 0 ;no cell has been selected
				
			For c.cell = Each cell
				If c\img <> 0
					If ImageRectCollide(c\img, c\x, c\y, 0, MouseX(), MouseY(), 1, 1)
						m\c = c
						m\selector = 1
						change = 1
						Exit
					EndIf
				EndIf
			Next
		
		ElseIf m\selector = 1 ;a cell has been selected
		
			For c.cell = Each cell
				If c\img <> 0
					If ImageRectCollide(c\img, c\x, c\y, 0, MouseX(), MouseY(), 1, 1)
						If m\c = c
							m\selector = 3
						Else
							m\c = c
							m\selector = 2
						EndIf
						change = 1
						Exit
					EndIf
				EndIf
			Next
		
		EndIf
	EndIf
	
	If MouseHit(2)
		m\hit = 2
		change = 1
	EndIf
	
	Return change

End Function

;miscellaneous
.miscellFuncs

Function checkSpecials(spec.special, b.board, undoSpecial=0)

	If spec\name = "en passant" ;;;
	
		If undoSpecial = 1 ;called from the undo function
			If b\prevMoves\nex <> Null
				b\prevMoves = b\prevMoves\nex
			Else
				Return
			EndIf
		EndIf
	
		If b\prevMoves\m <> Null
			If b\prevMoves\m\id = 101 Or b\prevMoves\m\id = 102 ;white/black pawn double-moves
			
				enemyId = 103-b\prevMoves\m\id ;2 (black) if white moves and vice versa
				leftSide = 2*(b\prevMoves\m\id-100)
				rightSide = 6-leftSide
				leftMoveId = 103 + leftSide
				rightMoveId = leftMoveId + 1
				
				If b\prevMoves\destCell\dullhorse[leftSide] <> Null ;the cell exists
					If b\prevMoves\destCell\dullhorse[leftSide]\p <> Null ;there's a piece next to it
						If b\prevMoves\destCell\dullhorse[leftSide]\p\id = enemyId ;black pawn
							If undoSpecial = 0 ;if I'm not undoing, add the move. Delete it otherwise.
								appendMove(b\prevMoves\destCell\dullhorse[leftSide]\p\m, fetchMoveById(leftMoveId))
							Else
								removeMove(b\prevMoves\destCell\dullhorse[leftSide]\p\m, leftMoveId)
							EndIf
						EndIf
					EndIf
				EndIf
				
				If b\prevMoves\destCell\dullhorse[rightSide] <> Null ;the cell exists
					If b\prevMoves\destCell\dullhorse[rightSide]\p <> Null ;there's a piece next to it
						If b\prevMoves\destCell\dullhorse[rightSide]\p\id = enemyId ;black pawn
							If undoSpecial = 0 ;if I'm not undoing, add the move. Delete it otherwise.
								appendMove(b\prevMoves\destCell\dullhorse[rightSide]\p\m, fetchMoveById(rightMoveId))
							Else
								removeMove(b\prevMoves\destCell\dullhorse[rightSide]\p\m, rightMoveId)
							EndIf
						EndIf
					EndIf
				EndIf
				
			ElseIf b\prevMoves\m\id >= 105 And b\prevMoves\m\id <= 108 ;en-passanted
				
				If b\prevMoves\capId = 0 ;if the capture hasn't yet been made
					nORs = 1 + 2*Int( (108-b\prevMoves\m\id)/2 )
				
					If b\prevMoves\destCell\dullhorse[nORs]\p <> Null ;make the capture
						b\prevMoves\capId = b\prevMoves\destCell\dullhorse[nORs]\p\id
						b\prevMoves\capx = b\prevMoves\destCell\dullhorse[nORs]\px
						b\prevMoves\capy = b\prevMoves\destCell\dullhorse[nORs]\py
						b\prevMoves\capP = b\prevMoves\destCell\dullhorse[nORs]\p
						b\prevMoves\capCell = b\prevMoves\destCell\dullhorse[nORs]
						b\prevMoves\capprevmvs = b\prevMoves\destCell\dullhorse[nORs]\p\m
						
						b\prevMoves\destCell\dullhorse[nORs]\p = Null
					Else
						DebugLog "Uh-oh! You done goofed!"
					EndIf
				EndIf
				
			EndIf
		EndIf
		
		If undoSpecial = 1
			b\prevMoves = b\prevMoves\bef
		EndIf
	
	ElseIf spec\name = "castling" ;;;
		
		If undoSpecial = 1
			If b\prevMoves\nex <> Null
				b\prevMoves = b\prevMoves\nex
			Else
				Return
			EndIf
		EndIf
		
		If b\prevMoves\m <> Null
		
			If b\prevMoves\m\id >= 11 And b\prevMoves\m\id <= 14 ;a rook moved
			
				;but which one?
				;check to the west first
				c.cell = b\prevMoves\srcCell
				While c\dullhorse[4] <> Null
					If c\dullhorse[4]\p <> Null
						If (c\dullhorse[4]\p\id = 11 And b\prevMoves\p\id = 7) Or (c\dullhorse[4]\p\id = 12 And b\prevMoves\p\id = 8) ;white king/rook or black king/rook
							
							If undoSpecial = 0
								removeMove(c\dullhorse[4]\p\m, 111)
							Else
								appendMove(c\dullhorse[4]\p\m, fetchMoveById(111))
							EndIf
							
						EndIf
						Exit
					EndIf
					c = c\dullhorse[4]
				Wend
				
				;now check to the east
				c = b\prevMoves\srcCell
				While c\dullhorse[2] <> Null
					If c\dullhorse[2]\p <> Null
						If (c\dullhorse[2]\p\id = 11 And b\prevMoves\p\id = 7) Or (c\dullhorse[2]\p\id = 12 And b\prevMoves\p\id = 8) ;white king/rook or black king/rook
							
							If undoSpecial = 0
								removeMove(c\dullhorse[2]\p\m, 110)
							Else
								appendMove(c\dullhorse[2]\p\m, fetchMoveById(110))
							EndIf
							
						EndIf
						Exit
					EndIf
					c = c\dullhorse[2]
				Wend
			
			ElseIf (b\prevMoves\m\id = 110 Or b\prevMoves\m\id = 111) And undoSpecial=0 ;castling has happened
			
				eORw = 2*(112-b\prevMoves\m\id) ;4 if 110, 2 if 111
				
				c.cell = b\prevMoves\destCell
				
				While c\dullhorse[eORw] <> Null
					
					If c\dullhorse[eORw]\p <> Null
						If c\dullhorse[eORw]\p\id = b\prevMoves\p\id-4
						
							b\prevMoves\other = New historyList
							movePiece(c\dullhorse[eORw], b\prevMoves\destCell\dullhorse[6-eORw], b\prevMoves\other)
							Exit
							
						EndIf
					EndIf
					
					c = c\dullhorse[eORw]
					
				Wend
			
			EndIf
		
		EndIf
		
		If undoSpecial = 1
			b\prevMoves = b\prevMoves\bef
		EndIf
	
	ElseIf spec\name = "promotion" ;;;
		
		If undoSpecial = 1
			If b\prevMoves\nex <> Null
				b\prevMoves = b\prevMoves\nex
			Else
				Return
			EndIf
		EndIf
		
		If b\prevMoves\playa = b\current
			
			If b\prevMoves\p <> Null
				If b\prevMoves\p\id = spec\params[1] ;this is a promotable piece
					
					offset = spec\params[0]+1
					offset = offset + spec\params[offset]+1
					onCell = 0
					
					For i = 1 To spec\params[offset]
						If spec\params[offset + i] = b\prevMoves\destCell\id
							onCell = 1
						EndIf
					Next
					
					offset = spec\params[0]+1
					
					If onCell = 1
					
						If undoSpecial = 0
							choice = choosePromotion(spec\params, b\prevMoves\destCell, fetchImageById(b\images, 1))
							
							temp.piece = b\prevMoves\destCell\p
							b\prevMoves\destCell\p = copyPiece( fetchPieceById(choice) )
							b\prevMoves\destCell\p\players = temp\players
						Else
							temp.piece = b\prevMoves\srcCell\p
							b\prevMoves\srcCell\p = copyPiece( fetchPieceById(spec\params[1]) )
							b\prevMoves\srcCell\p\players = temp\players
						EndIf
						
					EndIf
				
				EndIf
			EndIf
		
		EndIf
		
		If undoSpecial = 1
			b\prevMoves = b\prevMoves\bef
		EndIf
	
	Else
		DebugLog "That special does not exist."
	EndIf

End Function

Function checkAllSpecials(b.board, undoSpecial=0)

	speshes.specialsList = b\specials

	While speshes <> Null ;loop through the board's specials
	
		checkSpecials(speshes\s, b, undoSpecial) ;check the special moves
		
		If speshes\nex = Null
			Exit
		EndIf
		speshes = speshes\nex
	Wend

End Function

Function choosePromotion(params[1000], c.cell, pickImage)

	;stipulations: -params- and -c- must remain unchanged

	offset = params[0]+1
	;tempP.piece = copyPiece( fetchPieceById(params[offset+2]) )
	tempP.piece = Null
	choice.piece = fetchPieceById(params[0])
	
	k = params[offset] ;number of choices
	
	cw = ImageWidth(c\img)
	ch = ImageHeight(c\img)
	
	cornerX = c\x + -k*cw/2 + cw/2
	cornerY = c\y + 2*ch
	
	While Not KeyHit(14)
	
		DrawImage choice\img, c\x+c\px, c\y+c\py
	
		Color 200, 200, 200
		Rect cornerX-3, cornerY-3, k*cw+6, ch+6
		Color 100, 100, 100
		Rect cornerX, cornerY, k*cw, ch
		
		For i = 1 To k
			tempP = fetchPieceById(params[offset+i])
			DrawImage tempP\img, cornerX+(i-1)*cw + c\px, cornerY + c\py
		Next
		
		mx = MouseX()
		my = MouseY()
		
		If mx >= cornerX And mx < cornerX+(k)*cw And my >= cornerY And my <= cornerY+ch ;make sure the mouse is inside the box
		
			pick = (mx - cornerX)/cw
			
			DrawImage pickImage, cornerX + pick*cw, cornerY
			choice = fetchPieceById(params[offset+1+pick])
		
		EndIf
		
		If MouseHit(1)
			Return params[offset+1+pick]
		EndIf
		
		Flip
	
		If KeyHit(1) End
	Wend
	
	Return params[offset+1]

End Function

Function cellAttacked(b.board, target.cell, playa.player) ;if target is attacked by a piece that playa can control

	underAttack = 0
	
	cL.celloplayer = b\cells
	While cL <> Null And underAttack = 0
	
		If cL\c\p <> Null
			
			If playerInList(cL\c\p\players, playa) = 1

				hit.cellMoveList = possibleCells(cL\c, cL\c\p\m, b\ramifying, playa)
				
				While hit <> Null
					If debug = 1
						DebugLog " "+hit\c\id
					EndIf
					
					If hit\c = target
						If hit\srcmv\options[2] > 0 ;capturable
							underAttack = 1
							Exit
						EndIf
					EndIf
					
					If hit\nex = Null
						Exit
					EndIf
					hit = hit\nex
				Wend
				
				If hit <> Null
					;rewind and garbage-collect -hit-
					While hit\bef <> Null
						hit = hit\bef
					Wend
					While hit <> Null
						If hit\nex <> Null
							hit = hit\nex
							Delete hit\bef
						Else
							Delete hit
						EndIf
					Wend
				EndIf
				
			EndIf
			
		EndIf
		
		If cL\nex = Null
			Exit
		EndIf
		cL = cL\nex
	
	Wend
	
	Return underAttack

End Function

Function checkConditions(b.board, cml.cellMoveList, kind$)

	If kind = "castling"
	
		If cml <> Null
			If cml\srcmv <> Null
				If cml\srcmv\id = 110 Or cml\srcmv\id = 111 ;castling
				
					;empty cells between the king and rook
					eORw = 2*(112-cml\srcmv\id)
					c.cell = cml\c
					kingId = c\dullhorse[6-eORw]\dullhorse[6-eORw]\p\id
					
					success = 0
					
					While c\dullhorse[eORw] <> Null
						If c\dullhorse[eORw]\p <> Null
							If c\dullhorse[eORw]\p\id = kingId-4 ;same color
								success = 1
								Exit
							Else
								Return 0
							EndIf
						Else
							c = c\dullhorse[eORw]
						EndIf
					Wend
					
					;never in check
					c = cml\c
					
					While c <> Null And success = 1
						checkp.playerList = b\current\nex
						While checkp <> b\current
							If cellAttacked(b, c, checkp\p) = 1
								success = 0
								Exit
							EndIf
							checkp = checkp\nex
						Wend
						
						If c\p <> Null
							If c\p\id = kingId
								Exit
							EndIf
						EndIf
						
						c = c\dullhorse[6-eORw]
					Wend
					
					Return success
				
				Else
					Return 1
				EndIf
			Else
				Return 0
			EndIf
		Else
			Return 0
		EndIf
	
	Else
		DebugLog "That condition does not appear to exist."
	EndIf
	
	Return 1

End Function

Function checkVulnerable.celloplayer(b.board) ;returns cells with vulnerable pieces on them

	vuln.celloplayer = New celloplayer

	cL.celloplayer = b\cells
	While cL <> Null
		If cL\c\p <> Null
			If playerInList(cL\c\p\players, b\current\p) And cL\c\p\vulnerable = 1
				pL.playerList = b\current\nex
				While pL <> b\current
					If cellAttacked(b, cL\c, pL\p)
						
						If vuln\c <> Null
							vuln\nex = New celloplayer
							vuln\nex\bef = vuln
							vuln = vuln\nex
						EndIf
						
						vuln\c = cL\c
						
					EndIf
					pL = pL\nex
				Wend
			EndIf
		EndIf
		cL = cL\nex
	Wend
	
	If vuln\c = Null
		Delete vuln
	Else
		While vuln\bef <> Null
			vuln = vuln\bef
		Wend
	EndIf
	
	Return vuln

End Function

Function highlightVulnerable(b.board, highlightImageId)

	vuln.celloplayer = checkVulnerable(b)
	
	While vuln <> Null
		vuln\c\alt = fetchImageById(b\images, highlightImageId)
		If vuln\nex <> Null
			vuln = vuln\nex
			Delete vuln\bef
		Else
			Delete vuln
		EndIf
	Wend

End Function

; navigation ;