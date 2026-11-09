const $=id=>document.getElementById(id);
const API=(location.protocol==="http:"&&(location.hostname==="localhost"||location.hostname==="127.0.0.1"))?"http://localhost:8080/api":"https://mm-bank-backend-2ca5.onrender.com/api";
let ACCOUNT=null;
let balanceVisible=true,liveBalance=null,toastTimer,allTransactionData=[],currentFilter="ALL";

function toast(message){
 const el=$("toast");el.textContent=message;el.classList.add("show");
 clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove("show"),2500);
}
function money(value){
 return "R "+Number(value).toLocaleString("en-ZA",{minimumFractionDigits:2,maximumFractionDigits:2});
}
async function get(path){
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),45000);
 try{
  const response=await fetch(API+path,{signal:controller.signal,credentials:"include"});
  if(!response.ok)throw Error("HTTP "+response.status);
  return await response.json();
 }finally{clearTimeout(timer);}
}
function setStatus(connected){
 const el=document.querySelector(".status");
 el.innerHTML="<i></i> "+(connected?"Connected to bank":"Backend unavailable");
}
function transactionType(t){
 return String(t.type||"TRANSACTION").toUpperCase();
}
function isDebit(t){
 const type=transactionType(t);
 return type.includes("WITHDRAW")||type.includes("TRANSFER_SENT")||type.includes("TRANSFER_OUT");
}
function transactionRow(t){
 const type=transactionType(t),debit=isDebit(t);
 const row=document.createElement("div");row.className="transaction";
 const icon=document.createElement("div");icon.className="transaction-icon";icon.textContent=debit?"↗":"↓";
 const details=document.createElement("div");details.className="transaction-details";
 const title=document.createElement("strong");title.textContent=type.replaceAll("_"," ");
 const date=document.createElement("small");date.textContent=t.createdAt?new Date(t.createdAt).toLocaleString("en-ZA"):"Recent transaction";
 details.append(title,date);
 const amount=document.createElement("strong");amount.className=debit?"debit":"credit";
 amount.textContent=(debit?"− ":"+ ")+money(t.amount);
 row.append(icon,details,amount);return row;
}
function renderTransactions(items){
 const container=$("transactions");container.replaceChildren();
 if(!items.length){container.textContent="No transactions available.";return;}
 items.slice(0,5).forEach(t=>container.append(transactionRow(t)));
}
function renderActivity(){
 const container=$("allTransactions");
 if(!container)return;
 container.replaceChildren();
 const search=($("activitySearch")?.value||"").toLowerCase().trim();
 const filtered=allTransactionData.filter(t=>{
  const type=transactionType(t);
  const matchesType=currentFilter==="ALL"||
   (currentFilter==="DEPOSIT"&&type.includes("DEPOSIT"))||
   (currentFilter==="WITHDRAWAL"&&type.includes("WITHDRAW"))||
   (currentFilter==="TRANSFER"&&type.includes("TRANSFER"));
  const date=t.createdAt?new Date(t.createdAt).toLocaleString("en-ZA"):"";
  const matchesSearch=(type+" "+date+" "+t.amount).toLowerCase().includes(search);
  return matchesType&&matchesSearch;
 });
 $("activityCount").textContent=filtered.length+" transactions";
 if(!filtered.length){
  const empty=document.createElement("p");empty.className="empty-activity";
  empty.textContent=allTransactionData.length?"No matching transactions found.":"Your transactions will appear here.";
  container.append(empty);return;
 }
 filtered.forEach(t=>container.append(transactionRow(t)));
}
async function loadDashboard(){
 $("balance").textContent="Loading...";
 $("transactions").textContent="Loading transactions...";
 try{
  const [account,transactions]=await Promise.all([
   get("/mobile/account"),
   get("/mobile/transactions")
  ]);
  ACCOUNT=String(account.accountNumber);
 liveBalance=Number(account.balance);
  $("balance").textContent=balanceVisible?money(liveBalance):"••••••••";
  $("welcomeName").textContent=(account.fullName||account.accountHolderName||"Customer").split(" ")[0];
  const info=document.querySelectorAll(".account-info span");
  info[0].textContent=(account.accountType||"ACCOUNT")+" ACCOUNT";
  info[1].textContent="•••• "+ACCOUNT.slice(-4);
  allTransactionData=Array.isArray(transactions)?transactions:[];
  renderTransactions(allTransactionData);
  renderActivity();
  setStatus(true);
 }catch(error){
  $("balance").textContent="Unavailable";
  $("transactions").textContent="Unable to load transactions. Please try again.";
  $("allTransactions").textContent="Unable to load transaction history. Please try again.";
  setStatus(false);
  console.log("Bank connection:",error.message);
 }
}
$("toggleBalance").onclick=()=>{
 balanceVisible=!balanceVisible;
 $("balance").textContent=balanceVisible?(liveBalance===null?"Unavailable":money(liveBalance)):"••••••••";
 $("toggleBalance").textContent=balanceVisible?"HIDE":"SHOW";
};
function openTransaction(mode){
 document.querySelector('[data-tab="Transact"]').click();
 showTransactMode(mode);
}
const favouriteRoutes={
 "Transfer":()=>openTransaction("transfer"),
 "Payments":()=>openTransaction("payments"),
 "Airtime":()=>openTransaction("airtime"),
 "Statements":()=>document.querySelector('[data-tab="Activity"]').click(),
 "All transactions":()=>document.querySelector('[data-tab="Activity"]').click()
};
document.querySelectorAll("[data-action]").forEach(button=>{
 button.onclick=()=>{
  const action=button.dataset.action;
  if(favouriteRoutes[action])favouriteRoutes[action]();
  else toast(action+" will be available in a future stage.");
 };
});
document.querySelectorAll("[data-tab]").forEach(button=>{
 button.onclick=()=>{
  document.querySelectorAll("[data-tab]").forEach(b=>b.classList.remove("active"));
  button.classList.add("active");
  const tab=button.dataset.tab;
  const home=tab==="Home",cards=tab==="Cards",activity=tab==="Activity",transact=tab==="Transact",profile=tab==="Profile";
  $("cardsPage").hidden=!cards;
  $("activityPage").hidden=!activity;
  $("transactPage").hidden=!transact;
  $("profilePage").hidden=!profile;
  document.querySelector("main").hidden=!home;
  window.scrollTo(0,0);
  if(activity)renderActivity();
 };
});
let cardFrozen=false,cardRevealed=false;
$("revealCard").onclick=()=>{
 cardRevealed=!cardRevealed;
 $("cardDetails").hidden=!cardRevealed;
 $("cardNumber").textContent=cardRevealed?"5399 2400 1234 0001":"•••• •••• •••• 0001";
 $("cardExpiry").textContent=cardRevealed?"09/29":"••/••";
 $("revealCard").querySelector("strong").textContent=cardRevealed?"Hide details":"Show details";
};
$("freezeCard").onclick=()=>{
 cardFrozen=!cardFrozen;
 $("cardStatus").textContent=cardFrozen?"Card frozen":"Card active";
 $("cardStatusDot").classList.toggle("frozen",cardFrozen);
 $("freezeCard").querySelector("strong").textContent=cardFrozen?"Unfreeze card":"Freeze card";
 toast(cardFrozen?"Demo card frozen.":"Demo card unfrozen.");
};
$("profileBtn").onclick=()=>document.querySelector('[data-tab="Profile"]').click();
document.querySelectorAll("[data-profile-action]").forEach(button=>{
 button.onclick=()=>button.dataset.profileAction==="Sign out"?signOut():toast(button.dataset.profileAction+" will be available in a future stage.");
});
$("editFav").onclick=()=>toast("Favourite management is coming soon.");
if("serviceWorker" in navigator){
 window.addEventListener("load",()=>navigator.serviceWorker.register("sw.js?v=4").catch(()=>{}));
}
document.addEventListener("visibilitychange",()=>{
 if(!document.hidden&&document.body.classList.contains("authenticated"))loadDashboard();
});

let verifiedRecipient=null;
async function lookupRecipient(){
 const number=$("recipientAccount").value.trim();
 verifiedRecipient=null;
 $("recipientResult").hidden=true;
 $("transferSummary").hidden=true;
 $("confirmTransfer").disabled=true;
 if(!/^\d{10}$/.test(number)){toast("Enter a valid 10-digit account number.");return;}
 if(number===ACCOUNT){toast("You cannot transfer to your own account.");return;}
 try{
  const account=await get("/accounts/"+number);
  verifiedRecipient={number,name:account.accountHolderName||"Account holder"};
  $("recipientResult").textContent="Verified recipient: "+verifiedRecipient.name;
  $("recipientResult").hidden=false;
  updateTransferReview();
 }catch(e){toast("Account could not be verified.");}
}
function updateTransferReview(){
 if(!verifiedRecipient)return;
 const amount=Number($("transferAmount").value);
 const valid=Number.isFinite(amount)&&amount>0;
 $("confirmTransfer").disabled=!valid;
 if(valid){
  $("summaryRecipient").textContent=verifiedRecipient.name;
  $("summaryAmount").textContent=money(amount);
  $("transferSummary").hidden=false;
 }else $("transferSummary").hidden=true;
}
$("lookupRecipient").onclick=lookupRecipient;
$("transferAmount").addEventListener("input",updateTransferReview);
$("recipientAccount").addEventListener("input",()=>{
 verifiedRecipient=null;
 $("recipientResult").hidden=true;
 $("transferSummary").hidden=true;
 $("confirmTransfer").disabled=true;
});


const networkBundles={
 Vodacom:[["100 MB","R12",12],["250 MB","R25",25],["500 MB","R39",39],["1 GB","R65",65],["2 GB","R99",99],["5 GB","R199",199]],
 MTN:[["100 MB","R10",10],["250 MB","R24",24],["500 MB","R35",35],["1 GB","R59",59],["2 GB","R95",95],["5 GB","R189",189]],
 "Cell C":[["100 MB","R10",10],["250 MB","R20",20],["500 MB","R35",35],["1 GB","R55",55],["2 GB","R89",89],["5 GB","R179",179]],
 Telkom:[["100 MB","R9",9],["250 MB","R19",19],["500 MB","R29",29],["1 GB","R49",49],["2 GB","R79",79],["5 GB","R159",159]]
};
function showTransactMode(mode){
 document.querySelectorAll("[data-mode]").forEach(b=>b.classList.toggle("active",b.dataset.mode===mode));
 $("transferPanel").hidden=mode!=="transfer";
 $("otherTransactPanel").hidden=mode==="transfer";
 const panel=$("otherTransactPanel");
 if(mode==="payments"){
  panel.innerHTML=`
   <h3>Pay a bill</h3><p class="form-description">Enter your bill details below.</p>
   <form id="paymentForm">
    <label for="billCategory">Payment category</label>
    <select id="billCategory" required><option value="">Select category</option><option>Electricity</option><option>Water</option><option>Municipal services</option><option>Internet</option><option>Other</option></select>
    <label for="billReference">Beneficiary or reference number</label>
    <input id="billReference" required placeholder="Enter reference">
    <label for="billAmount">Amount (ZAR)</label>
    <input id="billAmount" type="number" min="1" step="0.01" required placeholder="0.00">
    <button class="primary-transfer" type="submit">Review payment</button>
   </form><p class="transfer-note">Demo interface. No payment will be processed.</p>`;
  $("paymentForm").onsubmit=e=>{
   e.preventDefault();
   const category=$("billCategory").value,reference=$("billReference").value.trim(),amount=Number($("billAmount").value);
   if(!category||!reference||!Number.isFinite(amount)||amount<=0)return;
   toast("Payment review: "+category+", "+money(amount)+". Backend integration pending.");
  };
 }
 if(mode==="airtime"){
  panel.innerHTML=`
   <h3>Airtime and data</h3>
   <p class="form-description">Choose a network and mobile purchase.</p>
   <form id="airtimeForm">
    <label for="mobileNumber">Cellphone number</label>
    <input id="mobileNumber" type="tel" inputmode="tel" required placeholder="e.g. 0821234567">
    <label for="network">Mobile network</label>
    <select id="network" required><option value="">Select network</option><option>Vodacom</option><option>MTN</option><option>Cell C</option><option>Telkom</option></select>
    <label for="bundleType">Purchase type</label>
    <select id="bundleType" required><option value="">Select purchase type</option><option value="Airtime">Airtime</option><option value="Data">Data bundle</option></select>
    <div id="airtimeAmountGroup" hidden>
     <label for="airtimeAmount">Select airtime amount</label>
     <select id="airtimeAmount"><option value="">Select amount</option><option value="10">R10</option><option value="20">R20</option><option value="30">R30</option><option value="50">R50</option><option value="100">R100</option><option value="200">R200</option></select>
    </div>
    <div id="dataBundleGroup" hidden>
     <label for="dataBundle">Choose data bundle</label>
     <select id="dataBundle"><option value="">Select data bundle</option></select>
     <div id="bundleInfo" class="bundle-info" hidden></div>
    </div>
    <button class="primary-transfer" type="submit">Review purchase</button>
   </form>
   <p class="transfer-note">Illustrative demo bundles and prices. Actual network tariffs may differ. No purchase will be processed.</p>`;
  const type=$("bundleType"),network=$("network"),airGroup=$("airtimeAmountGroup"),dataGroup=$("dataBundleGroup"),bundle=$("dataBundle"),info=$("bundleInfo");
  function updatePurchaseOptions(){
   const isData=type.value==="Data";
   airGroup.hidden=type.value!=="Airtime";
   dataGroup.hidden=!isData;
   bundle.innerHTML='<option value="">Select data bundle</option>';
   info.hidden=true;
   if(isData&&network.value){
    (networkBundles[network.value]||[]).forEach(item=>{
     const option=document.createElement("option");
     option.value=String(item[2]);
     option.textContent=item[0]+" - "+item[1];
     option.dataset.size=item[0];
     bundle.append(option);
    });
   }
  }
  type.onchange=updatePurchaseOptions;
  network.onchange=updatePurchaseOptions;
  bundle.onchange=()=>{
   const selected=bundle.options[bundle.selectedIndex];
   if(!bundle.value){info.hidden=true;return;}
   info.textContent=selected.dataset.size+" data bundle selected · "+money(bundle.value);
   info.hidden=false;
  };
  $("airtimeForm").onsubmit=e=>{
   e.preventDefault();
   const phone=$("mobileNumber").value.trim(),net=network.value,purchase=type.value;
   if(!/^(0[6-8][0-9]{8}|\+27[6-8][0-9]{8})$/.test(phone)){toast("Enter a valid South African cellphone number.");return;}
   if(!net||!purchase){toast("Select a network and purchase type.");return;}
   let amount,label;
   if(purchase==="Data"){
    if(!bundle.value){toast("Please select a data bundle.");return;}
    amount=Number(bundle.value);
    label=bundle.options[bundle.selectedIndex].textContent;
   }else{
    amount=Number($("airtimeAmount").value);
    if(!amount){toast("Please select an airtime amount.");return;}
    label=money(amount)+" airtime";
   }
   toast("Review: "+label+" for "+phone+" on "+net+". No purchase processed.");
  };
 }
}
document.querySelectorAll("[data-mode]").forEach(button=>{
 button.onclick=()=>showTransactMode(button.dataset.mode);
});
$("confirmTransfer").onclick=async()=>{
 if(!verifiedRecipient)return;
 const amount=Number($("transferAmount").value);
 if(!Number.isFinite(amount)||amount<=0){toast("Enter a valid amount.");return;}
 const confirmed=confirm("Transfer "+money(amount)+" to "+verifiedRecipient.name+"?\\nAccount: "+verifiedRecipient.number+"\\n\\nThis action will move funds in the demo banking system.");
 if(!confirmed)return;
 const button=$("confirmTransfer");
 button.disabled=true;button.textContent="Processing...";
 try{
  const result=await post("/mobile/transfer",{
   recipientAccountNumber:verifiedRecipient.number,amount
  });
  toast("Transfer completed successfully.");
  $("recipientAccount").value="";
  $("transferAmount").value="";
  $("recipientResult").hidden=true;
  $("transferSummary").hidden=true;
  verifiedRecipient=null;
  await loadDashboard();
 }catch(e){toast(e.message||"Transfer could not be completed.");}
 finally{button.disabled=!verifiedRecipient;button.textContent="Review transfer";}
};

/* M&M BANK AUTHENTICATION */
let csrfToken="";
async function refreshCsrf(){
 const r=await fetch(API+"/auth/csrf",{credentials:"include"});
 if(!r.ok)throw Error("Could not establish secure connection.");
 csrfToken=(await r.json()).token;
}
async function post(path,data){
 await refreshCsrf();
 const cookiePresent=document.cookie.split(";").some(x=>x.trim().startsWith("XSRF-TOKEN="));
 const r=await fetch(API+path,{method:"POST",credentials:"include",headers:{"Content-Type":"application/json","X-XSRF-TOKEN":csrfToken},body:JSON.stringify(data||{})});
 const result=await r.json().catch(()=>({}));
 if(!r.ok)throw Error("HTTP "+r.status+" | "+path+" | Origin: "+location.origin+" | CSRF cookie: "+cookiePresent+" | Token length: "+csrfToken.length+" | Response: "+JSON.stringify(result));
 return result;
}
function authMessage(message,error=true){
 $("authMessage").textContent=message;
 $("authMessage").style.color=error?"#ffaaaa":"#50c8f0";
}
function showAuth(){
 document.body.classList.remove("authenticated");
 $("authPage").hidden=false;
 $("loginForm").hidden=false;
 $("registerForm").hidden=true;
 $("authTitle").textContent="Welcome back";
 $("authDescription").textContent="Sign in securely to your account.";
 $("authSwitch").textContent="First time here? Activate your account";
 authMessage("");
}
$("authSwitch").onclick=()=>{
 const registering=$("registerForm").hidden;
 $("loginForm").hidden=registering;
 $("registerForm").hidden=!registering;
 $("authTitle").textContent=registering?"Activate your account":"Welcome back";
 $("authDescription").textContent=registering?"Use your customer and account details to get started.":"Sign in securely to your account.";
 $("authSwitch").textContent=registering?"Already registered? Sign in":"First time here? Activate your account";
 authMessage("");
};
$("loginForm").onsubmit=async e=>{
 e.preventDefault();
 const b=$("loginForm").querySelector("button");
 b.disabled=true;b.textContent="Signing in...";
 try{
  await post("/auth/login",{
   customerNumber:$("loginCustomer").value.trim(),
   password:$("loginPassword").value
  });
  await checkSession();
 }catch(err){authMessage(err.message);}
 finally{b.disabled=false;b.textContent="Sign in";}
};
$("registerForm").onsubmit=async e=>{
 e.preventDefault();
 const b=$("registerForm").querySelector("button");
 b.disabled=true;b.textContent="Activating...";
 try{
  await post("/auth/register",{
   customerNumber:$("regCustomer").value.trim(),
   accountNumber:$("regAccount").value.trim(),
   activationCode:$("regCode").value,
   password:$("regPassword").value
  });
  $("loginCustomer").value=$("regCustomer").value.trim();
  $("loginPassword").value="";
  $("registerForm").reset();
  $("loginForm").hidden=false;$("registerForm").hidden=true;
  $("authTitle").textContent="Account activated";
  $("authDescription").textContent="Your login is ready. Sign in with your new password.";
  $("authSwitch").textContent="First time here? Activate your account";
  authMessage("Registration successful. Please sign in.",false);
 }catch(err){authMessage(err.message);}
 finally{b.disabled=false;b.textContent="Activate account";}
};
async function checkSession(){
 try{
  const r=await fetch(API+"/auth/me",{credentials:"include"});
  if(!r.ok){showAuth();return;}
  document.body.classList.add("authenticated");
  $("authPage").hidden=true;
  await loadDashboard();
 }catch(e){
  showAuth();
  authMessage("Unable to connect. Check that the backend is running.");
 }
}
async function signOut(){
 try{
  await post("/auth/logout",{});
  ACCOUNT=null;
  $("loginPassword").value="";
  showAuth();
  toast("You have signed out.");
 }catch(e){toast("Sign out failed: "+e.message);}
}
checkSession();
