import os,re,sys,collections
B='com.example.vieva'
files={}
for root,_,fs in os.walk('com/example/vieva'):
    for f in fs:
        if f.endswith('.java'):
            p=os.path.join(root,f); src=open(p,encoding='utf-8',errors='ignore').read()
            pkg=re.search(r'^package\s+([\w.]+);',src,re.M).group(1)
            imps=re.findall(r'^import\s+(?:static\s+)?([\w.]+(?:\.\*)?);',src,re.M)
            files[pkg+'.'+f[:-5]]=(pkg,imps,src)
def inpkg(name,prefix): # prefix like a.b.. 
    pre=prefix.rstrip('.')
    return name==pre or name.startswith(pre+'.')
def deps(cls):
    pkg,imps,src=files[cls]; return imps
def rule(rid,desc,src_pkgs,dst_pkgs,exclude=()):
    v=[]
    for c,(pkg,imps,src) in files.items():
        if not any(inpkg(pkg,s) or inpkg(c,s) for s in src_pkgs): continue
        if any(inpkg(pkg,e) for e in exclude): continue
        for i in imps:
            if any(inpkg(i,d) for d in dst_pkgs): v.append((c,i))
    return rid,desc,v
D=B+'.domain';A=B+'.application';IP=A+'.ports.input';OP=A+'.ports.output';UC=A+'.usecases';AD=B+'.adapters';CT=AD+'.controllers';INF=B+'.infrastructure';ENT=D+'.entities'
FW=['org.springframework','jakarta.persistence','org.hibernate','com.fasterxml.jackson','jakarta.validation','jakarta.servlet','jakarta.transaction']
R=[rule('R1','domainDoesNotDependOnOuterLayers',[D],[A,AD,INF]),
rule('R2','domainHasZeroDependenciesOnFrameworks',[D],FW),
rule('R3','applicationDoesNotDependOnAdaptersInfraOrSpring',[A],[AD,INF]+FW),
rule('R4','outputPortsDoNotDependOnInputPorts',[OP],[IP]),
rule('R4b','inputPortsDoNotDependOnOutputPorts',[IP],[OP]),
rule('R4c','portsDoNotDependOnUseCaseImplementations',[IP,OP],[UC]),
rule('R5','adaptersDoNotDependOnInfrastructure',[AD],[INF]),
rule('R6','inputPortsDoNotDependOnDomainEntities',[IP],[ENT]),
rule('R7','controllersDoNotDependOnUseCaseImplementations',[CT],[UC]),
rule('R8','adaptersDoNotDependOnDomainEntities',[AD],[ENT]),
rule('R9','presentersDoNotDependOnDomainEntities',[AD+'.presenters'],[ENT]),
rule('R10','controllersDoNotDependOnDomainEntities',[CT],[ENT]),
rule('R11','controllersDoNotDependOnOutputPorts',[CT],[OP]),
rule('R12','adaptersDoNotDependOnOutputPorts',[AD],[OP]),
rule('R13','infrastructureDoesNotDependOnAdapters',[INF],[AD]),
rule('R14','infrastructureDatabaseDoesNotDependOnInputPorts',[INF+'.database'],[IP]),
rule('X1','aiGradingDoesNotDependOnInterviewEntities (de xuat)',[INF+'.ai'],[ENT+'.ExamAttempt',ENT+'.ExamTurn',ENT+'.Rubric',ENT+'.RubricCriterion']),
rule('X1b','aiGatewayPortsDoNotDependOnInterviewEntities (de xuat)',[OP+'.VivaEvaluationGateway',OP+'.AttemptGradingGateway',OP+'.RuleBasedGraderPort',OP+'.AttemptGradingResult'],[ENT+'.ExamAttempt',ENT+'.ExamTurn',ENT+'.Rubric',ENT+'.RubricCriterion']),
rule('X2','infrastructureDoesNotDependOnUseCases (de xuat, tru configuration)',[INF],[UC],[INF+'.configuration']),
]
# X3 public setters in entities via @Setter
x3=[c for c,(pkg,imps,src) in files.items() if inpkg(pkg,ENT) and re.search(r'^@Setter|^\s*@Setter\s*$',src,re.M)]
x3b=[c for c,(pkg,imps,src) in files.items() if inpkg(pkg,ENT) and re.search(r'public\s+void\s+set[A-Z]',src)]
for rid,desc,v in R:
    cls=sorted(set(c for c,_ in v))
    print(f"{rid}\t{desc}\t{'PASS' if not v else 'FAIL'}\t{len(cls)} class\t{len(v)} import")
    for c in cls[:40]: print('    ',c.replace(B+'.',''),'->',', '.join(sorted(set(i.replace(B+'.','') for cc,i in v if cc==c))))
print(f"X3\tentitiesHaveNoPublicSetters (de xuat)\t{'PASS' if not (x3 or x3b) else 'FAIL'}\t{len(set(x3+x3b))} class")
print('    ',', '.join(sorted(c.split('.')[-1] for c in set(x3+x3b))))
