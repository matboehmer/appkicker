/****************************************************************************
 *  This file is part of PPMd project                                       *
 *  Written and distributed to public domain by Dmitry Shkarin 1997,        *
 *  1999-2001                                                               *
 *  Contents: interface to encoding/decoding routines                       *
 *  Comments: this file can be used as an interface to PPMd module          *
 *  (consisting of Model.cpp) from external program           				*
 ****************************************************************************/
#if !defined(_MODEL_H_)
#define _MODEL_H_
#include "SubAlloc.hpp"
#define DEBUG 0
const int INT_BITS=7, PERIOD_BITS=7, TOT_BITS=INT_BITS+PERIOD_BITS,
INTERVAL=1 << INT_BITS, BIN_SCALE=1 << TOT_BITS, MAX_FREQ=124;


class PPMEncoder;
#pragma pack(1)
struct SEE2_CONTEXT { // SEE-contexts for PPM-contexts with masked symbols
    WORD Summ;
    BYTE Shift, Count;
    void init(int InitVal) { Summ=InitVal << (Shift=PERIOD_BITS-4); Count=4; }
    UINT getMean() {
        UINT RetVal=(Summ >> Shift);        Summ -= RetVal;
        return RetVal+(RetVal == 0);
    }
    void update() {
        if (Shift < PERIOD_BITS && --Count == 0) {
            Summ += Summ;                   Count=3 << Shift++;
        }
    }
} _PACK_ATTR; 

struct PPM_CONTEXT {
    WORD NumStats,SummFreq;                     // sizeof(WORD) > sizeof(BYTE)
    struct STATE { BYTE Symbol, Freq; PPM_CONTEXT* Successor; } _PACK_ATTR * Stats;
    PPM_CONTEXT* Suffix;
    PPMEncoder* encoder;
    int correct,incorrect;
    inline void encodeBinSymbol(int symbol);    // MaxOrder:
    inline void   encodeSymbol1(int symbol);    //  ABCD    context
    inline void   encodeSymbol2(int symbol);    //   BCD    suffix
    inline void           decodeBinSymbol();    //   BCDE   successor
    inline void             decodeSymbol1();    // other orders:
    inline void             decodeSymbol2();    //   BCD    context
    inline void           update1(STATE* p);    //    CD    suffix
    inline void           update2(STATE* p);    //   BCDE   successor
    void                          rescale();
    void   updateAccuracy(int symbol, int BUCKET);
    inline PPM_CONTEXT* createChild(STATE* pStats,STATE& FirstState);
    inline SEE2_CONTEXT* makeEscFreq2(int Diff);
    STATE& oneState()  const { return (STATE&) SummFreq; }
} _PACK_ATTR; 
#pragma pack()

class PPMEncoder {
public:
    long correct,incorrect,counter;
    int maxalphabet;
    PPM_CONTEXT* MinContext;
    PPM_CONTEXT* MaxContext;
    PPM_CONTEXT* FirstContext;
    PPM_CONTEXT* Sought;
    MemoryAllocator* memAllocator;
    SEE2_CONTEXT SEE2ContA[25][16], DummySEE2ContA;
    PPM_CONTEXT::STATE* FoundState;      // found next state transition
    int NumMasked, InitEsc, OrderFall, RunLength, InitRL, MaxOrder;
    BYTE CharMask[256], NS2Indx[256], NS2BSIndx[256], HB2Flag[256];
    BYTE EscCount, PrintCount, PrevSuccess, HiBitsFlag;
    WORD BinSumm[128][64];               // binary SEE-contexts
    void _STDCALL DecodeFile(_PPMD_FILE* DecodedFile,_PPMD_FILE* EncodedFile,int MaxOrder);
    void _STDCALL EncodeFile(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile,int MaxOrder);
    void ClearMask(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile);
    void UpdateModel();
    void _FASTCALL StartModelRare(int MaxOrder);
    void RestartModelRare();
    inline PPM_CONTEXT* CreateSuccessors(BOOL Skip,PPM_CONTEXT::STATE* p1);
    PPM_CONTEXT* _STDCALL SeekContext(int symbol);
    PPM_CONTEXT* _STDCALL SeekContext(short index, short contextLen,short MaxOrder,int context[]);
    void _STDCALL EncodeChar(int c, _PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile);
    void   setSymbolCounts(int symArray[],double freqArray[],int len, int BUCKET);
    void Initialize(int MaxOrder,int SASize);
    void DeInit(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile,BOOL verbose);
    double getWeightedProbability(int c);
};
//static PPMEncoder* encoder;
#endif /* !defined(_MODEL_H_) */
