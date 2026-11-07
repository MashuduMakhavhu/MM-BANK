
const API="http://localhost:8080/api";
const ACCOUNT="1000000001";
const TEST_PIN="1234";
const $=id=>document.getElementById(id);
const welcomeScreen=$("welcomeScreen"),cardScreen=$("cardScreen"),pinScreen=$("pinScreen"),menuScreen=$("menuScreen");
const pinDisplay=$("pinDisplay"),message=$("message"),attemptsDisplay=$("attempts");
const menuContainer=menuScreen.querySelector(".menu-container");
const originalMenu=menuContainer.innerHTML;
let enteredPIN="",attempts=3,currentAccount=null;

const styles=document.createElement("style");
styles.textContent=`
.bank-panel{background:#fff;color:#123b65;width:min(100%,570px);padding:27px 22px;border-radius:17px;box-shadow:0 20px 55px #001a3d44;text-align:center}
.bank-panel h2{margin:0 0 9px;font-size:23px}
.bank-panel p{color:#718096;font-size:13px;margin:8px 0 17px}
.bank-panel input{width:100%;padding:14px;margin:9px 0;border:1px solid #d9e4ef;border-radius:8px;font-size:16px;color:#123b65;outline:none}
.bank-panel input:focus{border-color:#0878cf;box-shadow:0 0 0 3px #0878cf18}
.bank-actions{display:flex;flex-wrap:wrap;justify-content:center;gap:10px;margin-top:18px}
.bank-btn{border:0;border-radius:8px;padding:13px 19px;font-weight:bold;background:#edf5fd;color:#07539b;cursor:pointer}
.bank-btn.primary{background:#0878cf;color:white}
.bank-btn.danger{background:#d99a25;color:white}
.bank-btn:disabled{opacity:.5}
.bank-note{font-size:13px;color:#64748b;margin:14px 0;min-height:20px}
.bank-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:9px;margin-top:15px}
.bank-grid .bank-btn{padding:15px 8px}
.bank-row{display:flex;justify-content:space-between;gap:12px;padding:12px 3px;border-bottom:1px solid #e5edf5;font-size:13px;text-align:left}
.bank-row strong{color:#07539b}
.bank-balance{font-size:30px;font-weight:800;color:#0878cf;margin:18px 0}
.bank-panel .bank-actions button{min-width:110px}
@media(max-width:450px){.bank-panel{padding:22px 13px}.bank-grid{gap:7px}.bank-grid .bank-btn{font-size:12px}.bank-balance{font-size:25px}}
`;
document.head.appendChild(styles);

function showScreen(screen){
 [welcomeScreen,cardScreen,pinScreen,menuScreen].forEach(s=>s.classList.add("hidden"));
 screen.classList.remove("hidden");
}
function money(value){
 return new Intl.NumberFormat("en-ZA",{style:"currency",currency:"ZAR"}).format(Number(value||0));
}
function safe(value){
 return String(value??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
}
async function api(path,options={}){
 const response=await fetch(API+path,{
  ...options,
  headers:{"Content-Type":"application/json",...(options.headers||{})}
 });
 const data=await response.json().catch(()=>null);
 if(!response.ok) throw new Error(data?.message||data?.error||`Bank server error (${response.status})`);
 return data;
}
async function refreshAccount(){
 currentAccount=await api("/accounts/"+encodeURIComponent(ACCOUNT));
 return currentAccount;
}
function updatePINDisplay(){
 pinDisplay.querySelectorAll("span").forEach((dot,i)=>{
  dot.textContent=i<enteredPIN.length?"●":"○";
 });
}
$("startButton").addEventListener("click",()=>{
 showScreen(cardScreen);
 setTimeout(()=>showScreen(pinScreen),1500);
});
document.querySelectorAll("[data-number]").forEach(button=>{
 button.addEventListener("click",()=>{
  if(enteredPIN.length<4&&attempts>0){
   enteredPIN+=button.dataset.number;
   updatePINDisplay();
  }
 });
});
$("clearButton").addEventListener("click",()=>{
 enteredPIN=enteredPIN.slice(0,-1);
 updatePINDisplay();
});
$("enterButton").addEventListener("click",async()=>{
 if(attempts<=0)return;
 if(enteredPIN.length!==4){message.textContent="Please enter all four digits.";return;}
 if(enteredPIN!==TEST_PIN){
  attempts--;
  enteredPIN="";
  updatePINDisplay();
  attemptsDisplay.textContent=attempts+" attempts remaining";
  message.textContent=attempts?"Incorrect PIN. Try again.":"Session locked. Please restart the ATM.";
  message.style.color="#d13b3b";
  if(!attempts)document.querySelectorAll(".key").forEach(k=>k.disabled=true);
  return;
 }
 const enter=$("enterButton");
 enter.disabled=true;
 message.textContent="Connecting securely to M&M Bank...";
 try{
  await refreshAccount();
  message.textContent="Account connected!";
  message.style.color="#16834a";
  setTimeout(renderMenu,500);
 }catch(e){
  message.textContent=e.message;
  message.style.color="#d13b3b";
 }finally{enter.disabled=false;}
});

function renderMenu(){
 menuContainer.innerHTML=originalMenu;
 const greeting=$("customerGreeting");
 if(greeting&&currentAccount)greeting.textContent="Welcome, "+currentAccount.accountHolderName;
 const buttons=menuContainer.querySelectorAll(".menu-option");
 buttons.forEach(button=>{
  const name=button.textContent.trim().toLowerCase();
  if(name.includes("withdraw"))button.addEventListener("click",withdrawScreen);
  else if(name.includes("deposit"))button.addEventListener("click",depositScreen);
  else if(name.includes("balance"))button.addEventListener("click",balanceScreen);
  else if(name.includes("transfer"))button.addEventListener("click",transferScreen);
  else if(name.includes("statement"))button.addEventListener("click",statementScreen);
 });
 $("exitButton").addEventListener("click",endSession);
 showScreen(menuScreen);
}
function panel(title,description,content){
 menuContainer.innerHTML=`
 <div class="bank-panel">
  <h2>${title}</h2><p>${description}</p>
  <div id="panelContent">${content}</div>
  <div class="bank-note" id="panelMessage"></div>
  <div class="bank-actions">
   <button class="bank-btn" id="backButton">BACK TO MENU</button>
   <button class="bank-btn danger" id="panelExit">END SESSION</button>
  </div>
 </div>`;
 $("backButton").onclick=renderMenu;
 $("panelExit").onclick=endSession;
}
function note(text,error=false){
 const el=$("panelMessage");
 if(el){el.textContent=text;el.style.color=error?"#d13b3b":"#16834a";}
}
function busy(button,state){
 if(button){button.disabled=state;button.textContent=state?"PLEASE WAIT...":"CONFIRM";}
}

// WITHDRAWAL
function withdrawScreen(){
 panel("WITHDRAW CASH","Choose your account type.",`
 <div class="bank-grid">
  <button class="bank-btn primary" data-type="SAVINGS">SAVINGS</button>
  <button class="bank-btn primary" data-type="CHEQUE">CHEQUE</button>
 </div>`);
 document.querySelectorAll("[data-type]").forEach(btn=>{
  btn.onclick=()=>{
   const selected=btn.dataset.type;
   if(selected!==currentAccount.accountType){
    note("This card is linked to a "+currentAccount.accountType+" account. "+selected+" is unavailable.",true);
    return;
   }
   withdrawalAmounts();
  };
 });
}
function withdrawalAmounts(){
 panel("WITHDRAW CASH","Select an amount or enter your own.",`
 <div class="bank-grid">
  ${[100,200,500,1000,2000,3000].map(n=>`<button class="bank-btn" data-amount="${n}">${money(n)}</button>`).join("")}
 </div>
 <input id="ownAmount" type="number" min="1" step="0.01" placeholder="Enter your own amount (R)">
 <div class="bank-actions"><button class="bank-btn primary" id="withdrawConfirm">CONFIRM WITHDRAWAL</button></div>`);
 let selected=0;
 document.querySelectorAll("[data-amount]").forEach(btn=>btn.onclick=()=>{
  selected=Number(btn.dataset.amount);
  $("ownAmount").value=selected;
  note("Selected "+money(selected));
 });
 $("withdrawConfirm").onclick=async()=>{
  const amount=Number($("ownAmount").value);
  if(!Number.isFinite(amount)||amount<=0){note("Enter a valid amount.",true);return;}
  if(amount>Number(currentAccount.balance)){note("Insufficient funds.",true);return;}
  const button=$("withdrawConfirm");
  button.disabled=true;button.textContent="PROCESSING...";
  try{
   const result=await api(`/accounts/${encodeURIComponent(ACCOUNT)}/withdraw`,{
    method:"POST",body:JSON.stringify({amount})
   });
   await refreshAccount();
   panel("WITHDRAWAL SUCCESSFUL","Please collect your cash.",`
    <div class="bank-balance">${money(result.amount)}</div>
    <div class="bank-row"><span>Account balance</span><strong>${money(currentAccount.balance)}</strong></div>
    <div class="bank-row"><span>Transaction</span><strong>#${safe(result.transactionId)}</strong></div>`);
  }catch(e){note(e.message,true);button.disabled=false;button.textContent="CONFIRM WITHDRAWAL";}
 };
}

// DEPOSIT
function depositScreen(){
 panel("CASH DEPOSIT","Enter the destination account number.",`
 <input id="depositAccount" inputmode="numeric" placeholder="Account number">
 <div class="bank-actions"><button class="bank-btn primary" id="findDepositAccount">FIND ACCOUNT</button></div>`);
 $("findDepositAccount").onclick=async()=>{
  const number=$("depositAccount").value.trim();
  if(!number){note("Enter an account number.",true);return;}
  try{
   const recipient=await api("/accounts/"+encodeURIComponent(number));
   depositAmountScreen(recipient);
  }catch(e){note(e.message,true);}
 };
}
function depositAmountScreen(recipient){
 panel("CONFIRM DEPOSIT",`Deposit into ${safe(recipient.accountHolderName)}?`,`
 <div class="bank-row"><span>Account number</span><strong>${safe(recipient.accountNumber)}</strong></div>
 <input id="depositAmount" type="number" min="0.01" step="0.01" placeholder="Amount in R">
 <div class="bank-actions"><button class="bank-btn primary" id="depositConfirm">CONFIRM DEPOSIT</button></div>`);
 $("depositConfirm").onclick=async()=>{
  const amount=Number($("depositAmount").value);
  if(!Number.isFinite(amount)||amount<=0){note("Enter a valid amount.",true);return;}
  const button=$("depositConfirm");button.disabled=true;button.textContent="PROCESSING...";
  try{
   const result=await api(`/accounts/${encodeURIComponent(recipient.accountNumber)}/deposit`,{
    method:"POST",body:JSON.stringify({amount})
   });
   if(recipient.accountNumber===ACCOUNT)await refreshAccount();
   panel("DEPOSIT SUCCESSFUL","The deposit has been recorded.",`
    <div class="bank-balance">${money(result.amount)}</div>
    <div class="bank-row"><span>Recipient</span><strong>${safe(recipient.accountHolderName)}</strong></div>
    <div class="bank-row"><span>Transaction</span><strong>#${safe(result.transactionId)}</strong></div>`);
  }catch(e){note(e.message,true);button.disabled=false;button.textContent="CONFIRM DEPOSIT";}
 };
}

// BALANCE
async function balanceScreen(){
 panel("BALANCE ENQUIRY","Retrieving your latest account balance...","");
 try{
  const data=await api(`/accounts/${encodeURIComponent(ACCOUNT)}/balance`);
  $("panelContent").innerHTML=`
   <div class="bank-row"><span>Account holder</span><strong>${safe(data.accountHolderName)}</strong></div>
   <div class="bank-row"><span>Account number</span><strong>${safe(data.accountNumber)}</strong></div>
   <div class="bank-row"><span>Account type</span><strong>${safe(data.accountType)}</strong></div>
   <div class="bank-balance">${money(data.balance)}</div>`;
 }catch(e){note(e.message,true);}
}

// TRANSFER
function transferScreen(){
 panel("TRANSFER FUNDS","Enter the receiving account number.",`
 <input id="transferAccount" inputmode="numeric" placeholder="Recipient account number">
 <div class="bank-actions"><button class="bank-btn primary" id="findTransferAccount">CONTINUE</button></div>`);
 $("findTransferAccount").onclick=async()=>{
  const number=$("transferAccount").value.trim();
  if(!number||number===ACCOUNT){note("Enter a different valid account number.",true);return;}
  try{
   const recipient=await api("/accounts/"+encodeURIComponent(number));
   transferAmountScreen(recipient);
  }catch(e){note(e.message,true);}
 };
}
function transferAmountScreen(recipient){
 panel("CONFIRM TRANSFER",`Send money to ${safe(recipient.accountHolderName)}?`,`
 <div class="bank-row"><span>Recipient account</span><strong>${safe(recipient.accountNumber)}</strong></div>
 <div class="bank-row"><span>Available balance</span><strong>${money(currentAccount.balance)}</strong></div>
 <input id="transferAmount" type="number" min="0.01" step="0.01" placeholder="Transfer amount (R)">
 <div class="bank-actions"><button class="bank-btn primary" id="transferConfirm">SEND MONEY</button></div>`);
 $("transferConfirm").onclick=async()=>{
  const amount=Number($("transferAmount").value);
  if(!Number.isFinite(amount)||amount<=0){note("Enter a valid amount.",true);return;}
  if(amount>Number(currentAccount.balance)){note("Insufficient funds.",true);return;}
  const button=$("transferConfirm");button.disabled=true;button.textContent="PROCESSING...";
  try{
   const result=await api(`/accounts/${encodeURIComponent(ACCOUNT)}/transfer`,{
    method:"POST",body:JSON.stringify({recipientAccountNumber:recipient.accountNumber,amount})
   });
   await refreshAccount();
   panel("TRANSFER SUCCESSFUL","Your transfer has been completed.",`
    <div class="bank-balance">${money(result.amount)}</div>
    <div class="bank-row"><span>Sent to</span><strong>${safe(recipient.accountHolderName)}</strong></div>
    <div class="bank-row"><span>New balance</span><strong>${money(currentAccount.balance)}</strong></div>`);
  }catch(e){note(e.message,true);button.disabled=false;button.textContent="SEND MONEY";}
 };
}

// MINI STATEMENT
async function statementScreen(){
 panel("MINI STATEMENT","Your latest banking transactions...","Loading transactions...");
 try{
  const transactions=await api(`/accounts/${encodeURIComponent(ACCOUNT)}/transactions`);
  if(!Array.isArray(transactions)||!transactions.length){
   $("panelContent").innerHTML="<p>No transactions found.</p>";return;
  }
  $("panelContent").innerHTML=transactions.map(t=>`
   <div class="bank-row">
    <span>${safe(t.type)}<br><small>${safe(String(t.createdAt||"").replace("T"," ").slice(0,16))}</small></span>
    <strong>${money(t.amount)}</strong>
   </div>`).join("");
 }catch(e){note(e.message,true);}
}

function endSession(){
 enteredPIN="";attempts=3;currentAccount=null;
 attemptsDisplay.textContent="3 attempts remaining";
 message.textContent="Enter your PIN to continue";
 message.style.color="";
 document.querySelectorAll(".key").forEach(k=>k.disabled=false);
 updatePINDisplay();
 menuContainer.innerHTML=originalMenu;
 showScreen(welcomeScreen);
}
showScreen(welcomeScreen);
