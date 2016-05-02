/****************************************************************************
 *  This file is part of PPMd project                                       *
 *  Written and distributed to public domain by Dmitry Shkarin 1997,        *
 *  1999-2001                                                               *
 *  Contents: interface to encoding/decoding routines                       *
 *  Comments: this file can be used as an interface to PPMd module          *
 *  (consisting of Model.cpp) from external program           				*
 ****************************************************************************/
#if !defined(_PPMD_H_)
#define _PPMD_H_

#include "PPMdType.h"


BOOL  _STDCALL StartSubAllocator(int SubAllocatorSize);
void  _STDCALL StopSubAllocator();          // it can be called once
DWORD _STDCALL GetUsedMemory();             // for information only

// (MaxOrder == 1) parameter value has special meaning, it does not restart
// model and can be used for solid mode archives;
// Call sequence:
//	StartSubAllocator(SubAllocatorSize);

// imported function
void _STDCALL  PrintInfo(_PPMD_FILE* DecodedFile,_PPMD_FILE* EncodedFile);

#endif /* !defined(_PPMD_H_) */
