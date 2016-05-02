/****************************************************************************
 *  This file is part of PPMd project                                       *
 *  Written and distributed to public domain by Dmitry Shkarin 1997,        *
 *  1999-2001                                                               *
 *  Contents: PPMII model description and encoding/decoding routines        *
 *  Comments: Adapted for Android by Abhinav Parate                         *
 ****************************************************************************/
#include <string.h>
#include "PPMd.h"
#include "Model.h"
#pragma hdrstop
#include "Coder.hpp"
#include <iostream>
#include <string>
#include <sstream>
using namespace std;

#define MY_BUCKET 5


inline PPM_CONTEXT* PPM_CONTEXT::createChild(STATE* pStats,STATE& FirstState)
{
    PPM_CONTEXT* pc = (PPM_CONTEXT*) encoder->memAllocator->AllocContext();
    if(DEBUG) cout<<"Child "<<pc<<" "<<this <<" "<<(this->Stats==pStats)<<" ";
    if ( pc ) {
        if(DEBUG) cout<<"Created";
        pc->encoder = this->encoder;
        pc->NumStats=1;                     pc->oneState()=FirstState;
        pc->Suffix=this;                    pStats->Successor=pc;
    }
    if(DEBUG) cout<<"\n";
    return pc;
}
void PPMEncoder::RestartModelRare()
{
    int i, k, m;
    memset(CharMask,0,sizeof(CharMask));
    memAllocator->InitSubAllocator();                     InitRL=-((MaxOrder < 12)?MaxOrder:12)-1;
    MinContext = MaxContext = (PPM_CONTEXT*) memAllocator->AllocContext();
    FirstContext = MinContext;
    MinContext->encoder = this;
    MinContext->Suffix=NULL;                OrderFall=MaxOrder;
    MinContext->SummFreq=(MinContext->NumStats=256)+1;
    FoundState = MinContext->Stats = (PPM_CONTEXT::STATE*) memAllocator->AllocUnits(256/2);
    for (RunLength=InitRL, PrevSuccess=i=0;i < 256;i++) {
        MinContext->Stats[i].Symbol=i;      MinContext->Stats[i].Freq=1;
        MinContext->Stats[i].Successor=NULL;
    }
    static const WORD InitBinEsc[]={0x3CDD,0x1F3F,0x59BF,0x48F3,0x64A1,0x5ABC,0x6632,0x6051};
    for (i=0;i < 128;i++)
        for (k=0;k < 8;k++)
            for (m=0;m < 64;m += 8)
                BinSumm[i][k+m]=BIN_SCALE-InitBinEsc[k]/(i+2);
    for (i=0;i < 25;i++)
        for (k=0;k < 16;k++)            SEE2ContA[i][k].init(5*i+10);
}
void _FASTCALL PPMEncoder::StartModelRare(int MaxOrder)
{
    int i, k, m ,Step;
    EscCount=PrintCount=1;
    if (MaxOrder < 2) {
        memset(CharMask,0,sizeof(CharMask));
        OrderFall=this->MaxOrder;               MinContext=MaxContext;
        while (MinContext->Suffix != NULL) {
            MinContext=MinContext->Suffix;  OrderFall--;
        }
        FoundState=MinContext->Stats;       MinContext=MaxContext;
    } else {
        this->MaxOrder=MaxOrder;                RestartModelRare();
        NS2BSIndx[0]=2*0;                   NS2BSIndx[1]=2*1;
        memset(NS2BSIndx+2,2*2,9);          memset(NS2BSIndx+11,2*3,256-11);
        for (i=0;i < 3;i++)                 NS2Indx[i]=i;
        for (m=i, k=Step=1;i < 256;i++) {
            NS2Indx[i]=m;
            if ( !--k ) { k = ++Step;       m++; }
        }
        memset(HB2Flag,0,0x40);             memset(HB2Flag+0x40,0x08,0x100-0x40);
        DummySEE2ContA.Shift=PERIOD_BITS;
    }
}
void PPM_CONTEXT::rescale()
{
    correct = correct/2;
    incorrect = incorrect/2;
    int OldNS=NumStats, i=NumStats-1, Adder, EscFreq;
    STATE* p1, * p;
    for (p=encoder->FoundState;p != Stats;p--)       _PPMD_SWAP(p[0],p[-1]);
    Stats->Freq += 4;                       SummFreq += 4;
    EscFreq=SummFreq-p->Freq;               Adder=(encoder->OrderFall != 0);
    SummFreq = (p->Freq=(p->Freq+Adder) >> 1);
    do {
        EscFreq -= (++p)->Freq;
        SummFreq += (p->Freq=(p->Freq+Adder) >> 1);
        if (p[0].Freq > p[-1].Freq) {
            STATE tmp=*(p1=p);
            do { p1[0]=p1[-1]; } while (--p1 != Stats && tmp.Freq > p1[-1].Freq);
            *p1=tmp;
        }
    } while ( --i );
    if (p->Freq == 0) {
        do { i++; } while ((--p)->Freq == 0);
        EscFreq += i;
        if ((NumStats -= i) == 1) {
            STATE tmp=*Stats;
            do { tmp.Freq-=(tmp.Freq >> 1); EscFreq>>=1; } while (EscFreq > 1);
            encoder->memAllocator->FreeUnits(Stats,(OldNS+1) >> 1);
            *(encoder->FoundState=&oneState())=tmp;  return;
        }
    }
    SummFreq += (EscFreq -= (EscFreq >> 1));
    int n0=(OldNS+1) >> 1, n1=(NumStats+1) >> 1;
    if (n0 != n1)
        Stats = (STATE*) encoder->memAllocator->ShrinkUnits(Stats,n0,n1);
    encoder->FoundState=Stats;
}
inline PPM_CONTEXT* PPMEncoder::CreateSuccessors(BOOL Skip,PPM_CONTEXT::STATE* p1)
{
    // static UpState declaration bypasses IntelC bug removed by ABHINAV
    PPM_CONTEXT::STATE UpState;
    PPM_CONTEXT* pc=MinContext, * UpBranch=FoundState->Successor;
    PPM_CONTEXT::STATE * p, * ps[MAX_O], ** pps=ps;
    if ( !Skip ) {
        *pps++ = FoundState;
        if ( !pc->Suffix )                  goto NO_LOOP;
    }
    if ( p1 ) {
        p=p1;                               pc=pc->Suffix;
        goto LOOP_ENTRY;
    }
    do {
        pc=pc->Suffix;
        if (pc->NumStats != 1) {
            if ((p=pc->Stats)->Symbol != FoundState->Symbol)
                do { p++; } while (p->Symbol != FoundState->Symbol);
        } else                              p=&(pc->oneState());
    LOOP_ENTRY:
        if (p->Successor != UpBranch) {
            pc=p->Successor;                break;
        }
        *pps++ = p;
    } while ( pc->Suffix );
NO_LOOP:
    if (pps == ps)                          return pc;
    UpState.Symbol=*(BYTE*) UpBranch;
    UpState.Successor=(PPM_CONTEXT*) (((BYTE*) UpBranch)+1);
    //BUG?
    //UpState.Successor->encoder= this;
    //cout<<"UPSTATE "<<UpState.Successor<<" "<<(int)UpState.Symbol<<" "<<UpState.Successor->NumStats<<endl;
    //BUG?
    if (pc->NumStats != 1) {
        if ((p=pc->Stats)->Symbol != UpState.Symbol)
            do { p++; } while (p->Symbol != UpState.Symbol);
        UINT cf=p->Freq-1;
        UINT s0=pc->SummFreq-pc->NumStats-cf;
        UpState.Freq=1+((2*cf <= s0)?(5*cf > s0):((2*cf+3*s0-1)/(2*s0)));
    } else                                  UpState.Freq=pc->oneState().Freq;
    do {
        pc = pc->createChild(*--pps,UpState);
        //BUG?
        pc->encoder = this;
        if ( !pc )                          return NULL;
    } while (pps != ps);
    return pc;
}
inline void PPMEncoder::UpdateModel()
{
    PPM_CONTEXT::STATE fs = *FoundState, * p = NULL;
    PPM_CONTEXT* pc, * Successor;
    UINT ns1, ns, cf, sf, s0;
    if (fs.Freq < MAX_FREQ/4 && (pc=MinContext->Suffix) != NULL) {
        if (pc->NumStats != 1) {
            if ((p=pc->Stats)->Symbol != fs.Symbol) {
                do { p++; } while (p->Symbol != fs.Symbol);
                if (p[0].Freq >= p[-1].Freq) {
                    _PPMD_SWAP(p[0],p[-1]); p--;
                }
            }
            if (p->Freq < MAX_FREQ-9) {
                p->Freq += 2;               pc->SummFreq += 2;
            }
        } else {
            p=&(pc->oneState());            p->Freq += (p->Freq < 32);
        }
    }
    if ( !OrderFall ) {
        MinContext=MaxContext=FoundState->Successor=CreateSuccessors(TRUE,p);
        if ( !MinContext )                  goto RESTART_MODEL;
        return;
    }
    *memAllocator->pText++ = fs.Symbol;                   Successor = (PPM_CONTEXT*) memAllocator->pText;
    Successor->NumStats=0;
    Successor->Stats = NULL;
    Successor->Suffix = NULL;
    Successor->encoder = this;
    if(DEBUG) cout<<"FoundState "<<FoundState<<" "<<(int)FoundState->Symbol<<endl;
    if(DEBUG) cout<<"MaxContext "<<MaxContext<<" "<<MaxContext->NumStats<<endl;
    if(DEBUG) cout<<"Successor "<<Successor<<" "<<Successor->NumStats<<endl;
    if (memAllocator->pText >= memAllocator->UnitsStart)                goto RESTART_MODEL;
    if ( fs.Successor ) {
        if ((BYTE*) fs.Successor <= memAllocator->pText &&
            (fs.Successor=CreateSuccessors(FALSE,p)) == NULL)
            goto RESTART_MODEL;
        if ( !--OrderFall ) {
            Successor=fs.Successor;         memAllocator->pText -= (MaxContext != MinContext);
        }
    } else {
        FoundState->Successor=Successor;    fs.Successor=MinContext;
    }
    s0=MinContext->SummFreq-(ns=MinContext->NumStats)-(fs.Freq-1);
    for (pc=MaxContext;pc != MinContext;pc=pc->Suffix) {
        if ((ns1=pc->NumStats) != 1) {
            if ((ns1 & 1) == 0) {
                pc->Stats=(PPM_CONTEXT::STATE*) memAllocator->ExpandUnits(pc->Stats,ns1 >> 1);
                if ( !pc->Stats )           goto RESTART_MODEL;
            }
            pc->SummFreq += (2*ns1 < ns)+2*((4*ns1 <= ns) &
                                            (pc->SummFreq <= 8*ns1));
        } else {
            p=(PPM_CONTEXT::STATE*) memAllocator->AllocUnits(1);
            if ( !p )                       goto RESTART_MODEL;
            *p=pc->oneState();              pc->Stats=p;
            if (p->Freq < MAX_FREQ/4-1)     p->Freq += p->Freq;
            else                            p->Freq  = MAX_FREQ-4;
            pc->SummFreq=p->Freq+InitEsc+(ns > 3);
        }
        cf=2*fs.Freq*(pc->SummFreq+6);      sf=s0+pc->SummFreq;
        if (cf < 6*sf) {
            cf=1+(cf > sf)+(cf >= 4*sf);
            pc->SummFreq += 3;
        } else {
            cf=4+(cf >= 9*sf)+(cf >= 12*sf)+(cf >= 15*sf);
            pc->SummFreq += cf;
        }
        p=pc->Stats+ns1;                    p->Successor=Successor;
        p->Symbol = fs.Symbol;              p->Freq = cf;
        pc->NumStats=++ns1;
    }
    MaxContext=MinContext=fs.Successor;
    //BUG?
    MinContext->encoder=this;
    return;
RESTART_MODEL:
    RestartModelRare();
    EscCount=0;                             PrintCount=0xFF;
}
// Tabulated escapes for exponential symbol distribution
static const BYTE ExpEscape[16]={ 25,14, 9, 7, 5, 5, 4, 4, 4, 3, 3, 3, 2, 2, 2, 2 };
#define GET_MEAN(SUMM,SHIFT,ROUND) ((SUMM+(1 << (SHIFT-ROUND))) >> (SHIFT))
inline void PPM_CONTEXT::encodeBinSymbol(int symbol)
{
    STATE& rs=oneState();                   encoder->HiBitsFlag=encoder->HB2Flag[encoder->FoundState->Symbol];
    WORD& bs=encoder->BinSumm[rs.Freq-1][encoder->PrevSuccess+encoder->NS2BSIndx[Suffix->NumStats-1]+
                                         encoder->HiBitsFlag+2*encoder->HB2Flag[rs.Symbol]+((encoder->RunLength >> 26) & 0x20)];
    if (rs.Symbol == symbol) {
        encoder->FoundState=&rs;                     rs.Freq += (rs.Freq < 128);
        SubRange.LowCount=0;                SubRange.HighCount=bs;
        bs += INTERVAL-GET_MEAN(bs,PERIOD_BITS,2);
        encoder->PrevSuccess=1;                      encoder->RunLength++;
    } else {
        SubRange.LowCount=bs;               bs -= GET_MEAN(bs,PERIOD_BITS,2);
        SubRange.HighCount=BIN_SCALE;       encoder->InitEsc=ExpEscape[bs >> 10];
        encoder->NumMasked=1;                        encoder->CharMask[rs.Symbol]=encoder->EscCount;
        encoder->PrevSuccess=0;                      encoder->FoundState=NULL;
    }
}
inline void PPM_CONTEXT::decodeBinSymbol()
{
    STATE& rs=oneState();                   encoder->HiBitsFlag=encoder->HB2Flag[encoder->FoundState->Symbol];
    WORD& bs=encoder->BinSumm[rs.Freq-1][encoder->PrevSuccess+encoder->NS2BSIndx[Suffix->NumStats-1]+
                                         encoder->HiBitsFlag+2*encoder->HB2Flag[rs.Symbol]+((encoder->RunLength >> 26) & 0x20)];
    if (ariGetCurrentShiftCount(TOT_BITS) < bs) {
        encoder->FoundState=&rs;                     rs.Freq += (rs.Freq < 128);
        SubRange.LowCount=0;                SubRange.HighCount=bs;
        bs += INTERVAL-GET_MEAN(bs,PERIOD_BITS,2);
        encoder->PrevSuccess=1;                      encoder->RunLength++;
    } else {
        SubRange.LowCount=bs;               bs -= GET_MEAN(bs,PERIOD_BITS,2);
        SubRange.HighCount=BIN_SCALE;       encoder->InitEsc=ExpEscape[bs >> 10];
        encoder->NumMasked=1;                        encoder->CharMask[rs.Symbol]=encoder->EscCount;
        encoder->PrevSuccess=0;                      encoder->FoundState=NULL;
    }
}
inline void PPM_CONTEXT::update1(STATE* p)
{
    (encoder->FoundState=p)->Freq += 4;              SummFreq += 4;
    if (p[0].Freq > p[-1].Freq) {
        _PPMD_SWAP(p[0],p[-1]);             encoder->FoundState=--p;
        if (p->Freq > MAX_FREQ)             rescale();
    }
}
inline void PPM_CONTEXT::updateAccuracy(int symbol, int BUCKET)
{
    if(symbol==0)
        return;
    encoder->counter++;
    if(encoder->counter<0)
        return;
    STATE *p=Stats;
    int numChars = NumStats -1;
    PPM_CONTEXT *context = this;//encoder->MinContext;
    int expectedChars = (BUCKET<encoder->maxalphabet?BUCKET:encoder->maxalphabet);
    BYTE found = 0;
    while(numChars<=expectedChars) {
        found = 0;
        if(context){
            p = context->Stats;
            for(int i=0;i<numChars;i++)
            {
                if(p->Symbol==symbol)
                    found = 1;
                ++p;
            }
            if(found)
                context->correct++;
            else
                context->incorrect++;
        }
        context = context->Suffix;
        numChars = context->NumStats-1;
    }
    found = 0;
    if(context) {
        p = context->Stats;
        BYTE freqs[numChars];
        BYTE symbols[numChars];
        int i=numChars,j=0;
        //Collecting frequency and symbols
        do  
        {   
            if(!p) break;
            freqs[i-1] =  p->Freq; symbols[i-1] = p->Symbol;
            ++p;
        }   
        while (--i);
        //Sorting
        for(i=0; i<numChars-1; i++) {
            for(j=0; j<numChars-i;j++)
                if(freqs[j]<freqs[j+1])
                {   
                    BYTE temp = freqs[j];
                    freqs[j] = freqs[j+1];
                    freqs[j+1] = temp;
                    temp = symbols[j];
                    symbols[j] = symbols[j+1];
                    symbols[j+1] = temp;
                }   
        }
        for(i=0;i<expectedChars;i++) {
            if(symbols[i]==symbol)
                found = 1;
        }
        if(found) encoder->correct++; else encoder->incorrect++;
        if(found) context->correct++; else context->incorrect++;
        /*if(encoder->counter%100==0){
         encoder->correct = encoder->correct/2;
         encoder->incorrect = encoder->incorrect/2;
         }*/
        //Avoids weird error of inaccessible memory
        int weird=encoder->incorrect;
    }
    else
        encoder->incorrect++;
}
double PPMEncoder::getWeightedProbability(int c)
{
    PPM_CONTEXT::STATE *p = MinContext->Stats;
    int numChars = MinContext->NumStats-1;
    PPM_CONTEXT *context = MinContext;
    
    double weight = (1.0*correct)/(correct+incorrect);
    if(correct+incorrect==0)
        weight=1.0;
    BOOL found = FALSE;
    while(!found) {
        p = context->Stats;
        numChars = context->NumStats-1;
        double w = weight;
        if(context->correct+context->incorrect!=0)
            w = (1.0*context->correct)/(context->correct+context->incorrect);
        for(int i=0;i<numChars;i++)
        {
            if(p->Symbol==c)
                return w*(1.0*p->Freq)/context->SummFreq;
            ++p;
        }
        context = context->Suffix;
        if(context==NULL)
            break;
    }
    //Exited without finding, not possible
    return -1.0;
}
#define FACTOR 0
void PPMEncoder::setSymbolCounts(int symArray[],double freqArray[],int len, int BUCKET)
{
    double nfreqArray[len];
    PPM_CONTEXT::STATE *p = MinContext->Stats;//Sought->Stats;
    int numChars = MinContext->NumStats-1;//Sought->NumStats -1;
    numChars = (Sought)->NumStats-1;
    PPM_CONTEXT *context = Sought;
    int expectedChars = (BUCKET<maxalphabet?BUCKET:maxalphabet);
    int checkedSymbols[256];
    for(int i=0;i<256;i++)
        checkedSymbols[i]=0;
    double weight = (1.0*correct)/(correct+incorrect);
    if(correct+incorrect==0)
        weight=1.0;
    double factor = 1.0;
    //Add for normalized scores
    double sumWeight = 0.0;
    while(numChars<=expectedChars) {
        p = context->Stats;
        double w = weight;
        if(context->correct+context->incorrect!=0)
            w = (1.0*context->correct)/(context->correct+context->incorrect);
        int SummFreq = 0;
        sumWeight += w;
        for(int i=0;i<numChars;i++)
        {
            if(!checkedSymbols[(int)p->Symbol])
                SummFreq += (int)p->Freq;
            ++p;
        }
        p=context->Stats;
        for(int i=0;i<numChars;i++)
        {
            BYTE symbol = p->Symbol;
            BYTE freq = p->Freq;
            
            if(!FACTOR || !checkedSymbols[(int)symbol]){
                for(int j=0;j<len;j++)
                {
                    if(FACTOR){
                        if(symArray[j]==symbol) { nfreqArray[j] += factor*w*(1.0*freq)/SummFreq; break;}
                        if(symArray[j]==-1) { symArray[j]=symbol; nfreqArray[j]=factor*w*(1.0*freq)/SummFreq;break;}
                    } else {
                        if(symArray[j]==symbol) { nfreqArray[j] += w*(1.0*freq)/context->SummFreq; break;}
                        if(symArray[j]==-1) { symArray[j]=symbol; nfreqArray[j]=w*(1.0*freq)/context->SummFreq;break;}
                    }
                }
                checkedSymbols[(int)symbol]=1;
            }
            ++p;
        }
        factor = factor*1.0/SummFreq;
        context = context->Suffix;
        numChars = context->NumStats-1;
    }
    if(context){
        p = context->Stats;
        double w = weight;
        if(context->correct+context->incorrect!=0)
            w = (1.0*context->correct)/(context->correct+context->incorrect);
        sumWeight+= w;
        numChars = context->NumStats-1;
        int SummFreq = 0;
        for(int i=0;i<numChars;i++)
        {
            if(!checkedSymbols[(int)p->Symbol])
                SummFreq += (int)p->Freq;
            ++p;
        }
        p=context->Stats;
        for(int i=0;i<numChars;i++)
        {
            BYTE symbol = p->Symbol;
            BYTE freq = p->Freq;
            if(!FACTOR || !checkedSymbols[(int)symbol]){
                for(int j=0;j<len;j++)
                {
                    if(FACTOR){
                        if(symArray[j]==symbol) { nfreqArray[j] += factor*w*(1.0*freq)/SummFreq;/*context->SummFreq;*/ break;}
                        if(symArray[j]==-1) { symArray[j]=symbol; nfreqArray[j]=factor*w*(1.0*freq)/SummFreq;/*context->SummFreq;*/break;}
                    } else {
                        if(symArray[j]==symbol) { nfreqArray[j] += w*(1.0*freq)/context->SummFreq; break;}
                        if(symArray[j]==-1) { symArray[j]=symbol; nfreqArray[j]=w*(1.0*freq)/context->SummFreq;break;}
                    }
                }
                checkedSymbols[(int)symbol]=1;
            }
            ++p;
        }
        context = context->Suffix;
    }
    if(sumWeight==0.0) sumWeight=1.0;
    for(int i=0;i<len;i++) {
        if(symArray[i]==-1) break;
        else{ nfreqArray[i] = nfreqArray[i]/sumWeight;
            if(nfreqArray[i]>1.0) nfreqArray[i]=1.0;
            freqArray[i] += nfreqArray[i];
        }
    }
}

inline void PPM_CONTEXT::encodeSymbol1(int symbol)
{
    SubRange.scale=SummFreq;
    STATE* p=Stats;
    
    if (p->Symbol == symbol) {
        encoder->PrevSuccess=(2*(SubRange.HighCount=p->Freq) > SubRange.scale);
        encoder->RunLength += encoder->PrevSuccess;
        (encoder->FoundState=p)->Freq += 4;          SummFreq += 4;
        if (p->Freq > MAX_FREQ)             rescale();
        SubRange.LowCount=0;                return;
    }
    encoder->PrevSuccess=0;
    int LoCnt=p->Freq, i=NumStats-1;
    while ((++p)->Symbol != symbol) {
        LoCnt += p->Freq;
        if (--i == 0) {
            encoder->HiBitsFlag=encoder->HB2Flag[encoder->FoundState->Symbol];
            SubRange.LowCount=LoCnt;        encoder->CharMask[p->Symbol]=encoder->EscCount;
            i=(encoder->NumMasked=NumStats)-1;       encoder->FoundState=NULL;
            do { encoder->CharMask[(--p)->Symbol]=encoder->EscCount; } while ( --i );
            SubRange.HighCount=SubRange.scale;
            return;
        }
    }
    SubRange.HighCount=(SubRange.LowCount=LoCnt)+p->Freq;
    update1(p);
}
inline void PPM_CONTEXT::decodeSymbol1()
{
    SubRange.scale=SummFreq;
    STATE* p=Stats;
    int i, count, HiCnt;
    if ((count=ariGetCurrentCount()) < (HiCnt=p->Freq)) {
        encoder->PrevSuccess=(2*(SubRange.HighCount=HiCnt) > SubRange.scale);
        encoder->RunLength += encoder->PrevSuccess;
        (encoder->FoundState=p)->Freq=(HiCnt += 4);  SummFreq += 4;
        if (HiCnt > MAX_FREQ)               rescale();
        SubRange.LowCount=0;                return;
    }
    encoder->PrevSuccess=0;                          i=NumStats-1;
    while ((HiCnt += (++p)->Freq) <= count)
        if (--i == 0) {
            encoder->HiBitsFlag=encoder->HB2Flag[encoder->FoundState->Symbol];
            SubRange.LowCount=HiCnt;        encoder->CharMask[p->Symbol]=encoder->EscCount;
            i=(encoder->NumMasked=NumStats)-1;       encoder->FoundState=NULL;
            do { encoder->CharMask[(--p)->Symbol]=encoder->EscCount; } while ( --i );
            SubRange.HighCount=SubRange.scale;
            return;
        }
    SubRange.LowCount=(SubRange.HighCount=HiCnt)-p->Freq;
    update1(p);
}
inline void PPM_CONTEXT::update2(STATE* p)
{
    (encoder->FoundState=p)->Freq += 4;              SummFreq += 4;
    if (p->Freq > MAX_FREQ)                 rescale();
    encoder->EscCount++;                             encoder->RunLength=encoder->InitRL;
}
inline SEE2_CONTEXT* PPM_CONTEXT::makeEscFreq2(int Diff)
{
    SEE2_CONTEXT* psee2c;
    if (NumStats != 256) {
        psee2c=encoder->SEE2ContA[encoder->NS2Indx[Diff-1]]+(Diff < Suffix->NumStats-NumStats)+
        2*(SummFreq < 11*NumStats)+4*(encoder->NumMasked > Diff)+encoder->HiBitsFlag;
        SubRange.scale=psee2c->getMean();
    } else {
        psee2c=&(encoder->DummySEE2ContA);              SubRange.scale=1;
    }
    return psee2c;
}
inline void PPM_CONTEXT::encodeSymbol2(int symbol)
{
    int HiCnt, i=NumStats-encoder->NumMasked;
    SEE2_CONTEXT* psee2c=makeEscFreq2(i);
    STATE* p=Stats-1;                       HiCnt=0;
    do {
        do { p++; } while (encoder->CharMask[p->Symbol] == encoder->EscCount);
        HiCnt += p->Freq;
        if (p->Symbol == symbol)            goto SYMBOL_FOUND;
        encoder->CharMask[p->Symbol]=encoder->EscCount;
    } while ( --i );
    SubRange.HighCount=(SubRange.scale += (SubRange.LowCount=HiCnt));
    psee2c->Summ += SubRange.scale;         encoder->NumMasked = NumStats;
    return;
SYMBOL_FOUND:
    SubRange.LowCount = (SubRange.HighCount=HiCnt)-p->Freq;
    if ( --i ) {
        STATE* p1=p;
        do {
            do { p1++; } while (encoder->CharMask[p1->Symbol] == encoder->EscCount);
            HiCnt += p1->Freq;
        } while ( --i );
    }
    SubRange.scale += HiCnt;
    psee2c->update();                       update2(p);
}
inline void PPM_CONTEXT::decodeSymbol2()
{
    int count, HiCnt, i=NumStats-encoder->NumMasked;
    SEE2_CONTEXT* psee2c=makeEscFreq2(i);
    STATE* ps[256], ** pps=ps, * p=Stats-1;
    HiCnt=0;
    do {
        do { p++; } while (encoder->CharMask[p->Symbol] == encoder->EscCount);
        HiCnt += p->Freq;                   *pps++ = p;
    } while ( --i );
    SubRange.scale += HiCnt;                count=ariGetCurrentCount();
    p=*(pps=ps);
    if (count < HiCnt) {
        HiCnt=0;
        while ((HiCnt += p->Freq) <= count) p=*++pps;
        SubRange.LowCount = (SubRange.HighCount=HiCnt)-p->Freq;
        psee2c->update();                   update2(p);
    } else {
        SubRange.LowCount=HiCnt;            SubRange.HighCount=SubRange.scale;
        i=NumStats-encoder->NumMasked;               pps--;
        do { encoder->CharMask[(*++pps)->Symbol]=encoder->EscCount; } while ( --i );
        psee2c->Summ += SubRange.scale;     encoder->NumMasked = NumStats;
    }
}
inline void PPMEncoder::ClearMask(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile)
{
    EscCount=1;                             memset(CharMask,0,sizeof(CharMask));
    if (++PrintCount == 0)                  PrintInfo(DecodedFile,EncodedFile);
}
void _STDCALL PPMEncoder::EncodeFile(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile,int MaxOrder)
{
    ariInitEncoder();                       StartModelRare(MaxOrder);
    for (int ns=MinContext->NumStats; ;ns=MinContext->NumStats) {
        int c = _PPMD_E_GETC(DecodedFile);
        if(c==0) {
            continue;
        }
        if(c>maxalphabet)
            maxalphabet = c;
        MinContext->updateAccuracy(c,MY_BUCKET);
        if (ns != 1) {
            MinContext->encodeSymbol1(c);   ariEncodeSymbol();
        } else {
            MinContext->encodeBinSymbol(c); ariShiftEncodeSymbol(TOT_BITS);
        }
        while ( !FoundState ) {
            //NO FILE OP
            //ARI_ENC_NORMALIZE(EncodedFile);
            do {
                OrderFall++;                MinContext=MinContext->Suffix;
                if ( !MinContext )          goto STOP_ENCODING;
            } while (MinContext->NumStats == NumMasked);
            MinContext->encodeSymbol2(c);   ariEncodeSymbol();
        }
        if (!OrderFall && (BYTE*) FoundState->Successor > memAllocator->pText)
            MinContext=MaxContext=FoundState->Successor;
        else {
            UpdateModel();
            if (EscCount == 0)              ClearMask(EncodedFile,DecodedFile);
        }
        //NO FILE OP
        //ARI_ENC_NORMALIZE(EncodedFile);
    }
STOP_ENCODING:
    //NO FILE OP
    /*ARI_FLUSH_ENCODER(EncodedFile);*/
    if(0) {
        PrintInfo(DecodedFile,EncodedFile);
        char WrkStr[320];
        sprintf(WrkStr,"Total %ld:  Correct %ld, Incorrect %ld, Charsize %lu, Accuracy %2.2f",
                counter,correct,incorrect,sizeof(char),((float)correct*100.0)/(correct+incorrect));
        printf("%-79.99s\n",WrkStr);
    }
    counter = correct=incorrect=0;
}
void _STDCALL PPMEncoder::DecodeFile(_PPMD_FILE* DecodedFile,_PPMD_FILE* EncodedFile,int MaxOrder)
{
    ARI_INIT_DECODER(EncodedFile);          StartModelRare(MaxOrder);
    for (int ns=MinContext->NumStats; ;ns=MinContext->NumStats) {
        if (ns != 1)                        MinContext->decodeSymbol1();
        else                                MinContext->decodeBinSymbol();
        ariRemoveSubrange();
        while ( !FoundState ) {
            ARI_DEC_NORMALIZE(EncodedFile);
            do {
                OrderFall++;                MinContext=MinContext->Suffix;
                if ( !MinContext )          goto STOP_DECODING;
            } while (MinContext->NumStats == NumMasked);
            MinContext->decodeSymbol2();    ariRemoveSubrange();
        }
        _PPMD_D_PUTC(FoundState->Symbol,DecodedFile);
        if (!OrderFall && (BYTE*) FoundState->Successor > memAllocator->pText)
            MinContext=MaxContext=FoundState->Successor;
        else {
            UpdateModel();
            if (EscCount == 0)              ClearMask(EncodedFile,DecodedFile);
        }
        ARI_DEC_NORMALIZE(EncodedFile);
    }
STOP_DECODING:
    PrintInfo(DecodedFile,EncodedFile);
}

PPM_CONTEXT* _STDCALL PPMEncoder::SeekContext(int symbol)
{
    
    PPM_CONTEXT* ctx = MinContext;
    BOOL foundContext = true;
    
    //Check for null
    if(ctx==NULL || !ctx){
        if(DEBUG) cout<<"Encountered Null\n";
        return NULL;
    }
    //Stats in current context
    PPM_CONTEXT::STATE *p = ctx->Stats;
    int numStats = ctx->NumStats;
    BOOL foundSymbol = false;
    if(numStats>1){
        int numChars = numStats-1;
        
        //Search symbol in stats
        for(int k=0;k<numChars+1;k++){
            if(p==NULL || !p) break;
            if(DEBUG) cout<<"P"<<(int)p->Symbol<<" "<<p->Successor<<" ";
            if(p->Symbol == symbol)
            {
                foundSymbol = true; break;
            }
            ++p;
        }
        if(DEBUG) cout<<"\n";
    }
    if(foundSymbol) {ctx = p->Successor;}
    //Else break out of the for loop
    else {
        foundContext = false; 
        if(DEBUG) cout<<"Not found symbol "<<symbol<<"\n";
    }        
    //check if context is found, else drop order
    if(foundContext&&foundSymbol && ctx)
    {
        //Get context with nonzero stats
        while(ctx->NumStats==0 || !ctx->Stats){
            ctx = ctx->Suffix;
            if(!ctx) break;
        }
        if(ctx) { 
            if(ctx->NumStats>=257){
                if(DEBUG) cout<<"Returning First\n"; return FirstContext;
            }
        }
        if(DEBUG) if(ctx) cout<<"Returning Context "<<ctx<<"\n";
        if(!ctx || !ctx->Stats) return NULL;
        if(DEBUG) cout<<"Returning Context "<<ctx<<"\n";
        if(ctx) return ctx;
    }
    //return null if got nothing
    if(DEBUG) cout<<"Returning First Context\n";
    return FirstContext;
}

PPM_CONTEXT* _STDCALL PPMEncoder::SeekContext(short index, short contextLen,short CtxOrder,int context[])
{
    index = (contextLen<CtxOrder?0:index);
    
    if(DEBUG){ cout<<"Series ";
        for(int i=0;i<contextLen;i++)
            cout<<context[(index+i)%CtxOrder]<<" "; cout<<"\n"; }
    
    PPM_CONTEXT* ctxArr[5];
    
    //iterate through the order, drop if required
    for(int i=contextLen;i>0;i--) {
        if(DEBUG) cout<<"Scanning Order "<<i<<"\n";
        
        PPM_CONTEXT* ctx = FirstContext;
        BOOL foundContext = true;
        
        if(DEBUG) if(!ctx->encoder) cout<<"Empty Encoder \n";
        
        //Expand context prefix, character by character
        for(int j=0;j<contextLen-1;j++)
        {
            ctxArr[j] = ctx;
            //Check for null
            if(ctx==NULL || !ctx){
                if(DEBUG) cout<<"Encountered Null\n";
                foundContext = 0; break;
            }
            //Symbol to append
            int symbol = context[(index+j)%CtxOrder];
            //Stats in current context
            PPM_CONTEXT::STATE *p = ctx->Stats;
            int numStats = ctx->NumStats;
            if(DEBUG) cout<<"-Symbol "<<symbol<<" Ctx:"<<ctx
                <<" Nchars:"<<numStats<<"\n-";
            /*if(numStats>257){
             ctx->NumStats=0;
             ctx->Stats=NULL;
             foundContext= false;
             break;
             }*/
            //Stats>1
            BOOL foundSymbol = false;
            if(numStats>1){
                int numChars = numStats-1;
                
                //Search symbol in stats
                BOOL firstTime = false;
                BOOL sanityCheck = (ctx==FirstContext);
                for(int k=0;k<numChars+1;k++){
                    if(p==NULL || !p) break;
                    if(DEBUG) cout<<"P"<<(int)p->Symbol<<" "<<p->Successor<<" ";
                    if(p->Symbol == symbol)
                    {
                        if(sanityCheck && firstTime) { firstTime=false; ++p;continue;}
                        if(DEBUG) cout<<"\n-Found Symbol "<<symbol<<" Ctx:"<<p->Successor
                            <<" Nchars:"<<p->Successor->NumStats<<" "<<k<<"\n";
                        foundSymbol = true; break;
                    }
                    ++p;
                }
                if(DEBUG) cout<<"\n";
            } else if(numStats == 1) { //Binary Stat
                //if(p->Symbol == symbol)
                //foundSymbol = true;
            } else {} // 0 stats
            //Continue to expand prefix if found symbol
            if(foundSymbol) {ctx = p->Successor;}
            //Else break out of the for loop
            else {
                foundContext = false; 
                if(DEBUG) cout<<"Not found symbol "<<symbol<<"\n";
                break;
            }
        } //end prefix expansion loop
        
        //check if context is found, else drop order
        if(foundContext && ctx)
        {
            //Get context with nonzero stats
            while(ctx->NumStats==0 || !ctx->Stats){
                ctx = ctx->Suffix;
                if(!ctx) break;
            }
            if(DEBUG) if(ctx) cout<<"Returning Context "<<ctx<<"\n";
            if(ctx) return ctx;
            else {
                if(DEBUG) cout<<"Found Context became empty\n";
                index++; contextLen--;
            }
        }
        else {index++; contextLen--;}
    }
    //return first if got nothing
    if(DEBUG) cout<<"Returning First Context "<<FirstContext<<"\n";
    return FirstContext;
}

void PPMEncoder::Initialize(int MaxOrder,int SASize)
{
    memAllocator = new MemoryAllocator();
    memAllocator->Init();
    memAllocator->StartSubAllocator(SASize);
    counter = 1;correct=0;incorrect=0;maxalphabet = -1;
    ariInitEncoder();
    StartModelRare(MaxOrder);
}

void PPMEncoder::DeInit(_PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile, BOOL verbose)
{
    //NO FILE OP
    /*ARI_FLUSH_ENCODER(EncodedFile);*/
    if(verbose){
        PrintInfo(DecodedFile,EncodedFile);
        char WrkStr[320];
        sprintf(WrkStr,"Total: %ld,  Correct: %ld, Incorrect: %ld, Charsize: %lu, Accuracy: %2.2f",
                correct+incorrect,correct,incorrect,sizeof(char),((float)correct*100.0)/(correct+incorrect));
        counter = correct=incorrect=0; counter=1;
        printf("%-79.99s",WrkStr);
    }
    memAllocator->StopSubAllocator();
    delete memAllocator;
}

void _STDCALL PPMEncoder::EncodeChar(int c, _PPMD_FILE* EncodedFile,_PPMD_FILE* DecodedFile)
{
    if(c==0)
        return;
    if(DEBUG) {
        cout<<MinContext<<" "<<Sought<<" "<<MinContext->Suffix<<" "<<(MinContext==Sought?"SS":"DD")<<" "<<(MinContext->Suffix==Sought?"SS":"DD")<<"\n";}
    
    if(c>maxalphabet)
        maxalphabet = c;
    //MinContext = Sought;
    MinContext->encoder = this;
    int ns=MinContext->NumStats;
    if (ns != 1) {
        MinContext->encodeSymbol1(c);   ariEncodeSymbol();
    } else {
        MinContext->encodeBinSymbol(c); ariShiftEncodeSymbol(TOT_BITS);
    }
    while ( !FoundState ) {
        //NO FILE OP
        //ARI_ENC_NORMALIZE(EncodedFile);
        do {
            OrderFall++;                MinContext=MinContext->Suffix;
            if ( !MinContext )          goto STOP_ENCODING;
            //BUG?
            MinContext->encoder = this;
        } while (MinContext->NumStats <= NumMasked);
        MinContext->encodeSymbol2(c);   ariEncodeSymbol();
    }
    if (!OrderFall && (BYTE*) FoundState->Successor > memAllocator->pText)
        MinContext=MaxContext=FoundState->Successor;
    else {
        UpdateModel();
        if (EscCount == 0)              ClearMask(EncodedFile,DecodedFile);
        if(DEBUG) cout<<"Updated\n";
        
    }
    //NO FILE OP
    //ARI_ENC_NORMALIZE(EncodedFile);
STOP_ENCODING:
    return;
}


